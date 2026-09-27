import http from 'k6/http';
import { fail } from 'k6';
import { getConfig } from './config.js';
import { authHeaders, checkOk, checkCreated } from './http.js';
import { ensureAuth } from './auth.js';

function firstId(list) {
  if (!Array.isArray(list) || list.length === 0) {
    return null;
  }
  return list[0].id;
}

function uniqueSuffix() {
  // VU/iter + time + random → avoid collisions across rapid re-runs
  const rand = Math.floor(Math.random() * 1e6)
    .toString()
    .padStart(6, '0');
  return `${String(Date.now()).slice(-8)}${rand}`.slice(0, 14);
}

function createWorkspace(baseUrl, headers) {
  const suffix = uniqueSuffix();
  const create = http.post(
    `${baseUrl}/api/v1/workspaces`,
    JSON.stringify({
      name: `k6 Workspace ${suffix}`,
      workspaceKey: `K6W${suffix}`.toUpperCase().slice(0, 20),
      description: 'Created by k6 setup (ephemeral)',
    }),
    { headers, tags: { name: 'POST /workspaces (setup)' } },
  );
  checkCreated(create, 'setup create workspace');
  const id = create.json('data.id');
  if (!id) {
    fail(`Failed to create workspace: status=${create.status} body=${String(create.body).slice(0, 300)}`);
  }
  return id;
}

function createProject(baseUrl, headers, workspaceId) {
  const suffix = uniqueSuffix();
  const create = http.post(
    `${baseUrl}/api/v1/workspaces/${workspaceId}/projects`,
    JSON.stringify({
      name: `k6 Project ${suffix}`,
      projectKey: `K6P${suffix}`.toUpperCase().slice(0, 20),
      description: 'Created by k6 setup (ephemeral)',
      projectType: 'SOFTWARE',
    }),
    { headers, tags: { name: 'POST /projects (setup)' } },
  );
  checkCreated(create, 'setup create project');
  const id = create.json('data.id');
  if (!id) {
    fail(`Failed to create project: status=${create.status} body=${String(create.body).slice(0, 300)}`);
  }
  return id;
}

function discoverOrCreateWorkspace(baseUrl, headers, workspaceId) {
  if (workspaceId) {
    return workspaceId;
  }

  const res = http.get(`${baseUrl}/api/v1/workspaces`, {
    headers,
    tags: { name: 'GET /workspaces (setup)' },
  });
  checkOk(res, 'setup workspaces');
  const existing = firstId(res.json('data'));
  if (existing) {
    return existing;
  }

  return createWorkspace(baseUrl, headers);
}

function discoverOrCreateProject(baseUrl, headers, workspaceId, projectId) {
  if (projectId) {
    return projectId;
  }

  const res = http.get(`${baseUrl}/api/v1/workspaces/${workspaceId}/projects`, {
    headers,
    tags: { name: 'GET /projects (setup)' },
  });
  checkOk(res, 'setup projects');
  const existing = firstId(res.json('data'));
  if (existing) {
    return existing;
  }

  return createProject(baseUrl, headers, workspaceId);
}

function discoverIssue(baseUrl, headers, projectId, issueId) {
  if (issueId) {
    return issueId;
  }
  const res = http.get(`${baseUrl}/api/v1/projects/${projectId}/issues?page=0&size=1`, {
    headers,
    tags: { name: 'GET /issues (setup)' },
  });
  checkOk(res, 'setup issues');
  const page = res.json('data');
  return firstId(page?.data);
}

function loadReferenceIds(baseUrl, headers) {
  const [typesRes, statusesRes, prioritiesRes] = [
    http.get(`${baseUrl}/api/v1/ref/issue-types`, { headers, tags: { name: 'GET /ref/issue-types (setup)' } }),
    http.get(`${baseUrl}/api/v1/ref/statuses`, { headers, tags: { name: 'GET /ref/statuses (setup)' } }),
    http.get(`${baseUrl}/api/v1/ref/priorities`, { headers, tags: { name: 'GET /ref/priorities (setup)' } }),
  ];

  checkOk(typesRes, 'setup issue-types');
  checkOk(statusesRes, 'setup statuses');
  checkOk(prioritiesRes, 'setup priorities');

  const taskType =
    (typesRes.json('data') || []).find((t) => t.name === 'TASK') ||
    (typesRes.json('data') || [])[0];
  const todoStatus =
    (statusesRes.json('data') || []).find((s) => s.name === 'TO DO') ||
    (statusesRes.json('data') || [])[0];
  const mediumPriority =
    (prioritiesRes.json('data') || []).find((p) => p.name === 'MEDIUM') ||
    (prioritiesRes.json('data') || [])[0];

  return {
    issueTypeId: taskType?.id,
    statusId: todoStatus?.id,
    priorityId: mediumPriority?.id,
  };
}

/**
 * @param {{ ephemeral?: boolean }} [options]
 * ephemeral=true → always create a fresh workspace+project so teardown can delete them.
 */
export function setupTestData(options = {}) {
  const cfg = getConfig();
  const ephemeral =
    options.ephemeral === true ||
    __ENV.K6_EPHEMERAL === 'true' ||
    __ENV.K6_EPHEMERAL === '1';

  const health = http.get(`${cfg.baseUrl}/actuator/health`, {
    tags: { name: 'GET /actuator/health (setup)' },
  });
  if (health.status !== 200) {
    fail(`Backend not reachable at ${cfg.baseUrl} (health=${health.status})`);
  }

  const token = ensureAuth();
  const headers = authHeaders(token);

  let workspaceId;
  let projectId;
  let createdWorkspace = false;
  let createdProject = false;

  if (ephemeral) {
    if (cfg.workspaceId) {
      workspaceId = cfg.workspaceId;
    } else {
      workspaceId = createWorkspace(cfg.baseUrl, headers);
      createdWorkspace = true;
    }
    if (cfg.projectId) {
      // Explicit PROJECT_ID with ephemeral: do not delete caller's project in teardown
      projectId = cfg.projectId;
    } else {
      projectId = createProject(cfg.baseUrl, headers, workspaceId);
      createdProject = true;
    }
  } else {
    workspaceId = discoverOrCreateWorkspace(cfg.baseUrl, headers, cfg.workspaceId);
    projectId = discoverOrCreateProject(cfg.baseUrl, headers, workspaceId, cfg.projectId);
  }

  const issueId = discoverIssue(cfg.baseUrl, headers, projectId, cfg.issueId);
  const refs = loadReferenceIds(cfg.baseUrl, headers);
  const sprintId = discoverOrCreateActiveSprint(cfg.baseUrl, headers, projectId);

  if (!refs.issueTypeId || !refs.statusId || !refs.priorityId) {
    fail('Reference data missing (issue-types / statuses / priorities). Check Flyway seed migrations.');
  }

  console.log(
    `setup ok: ephemeral=${ephemeral} workspace=${workspaceId} project=${projectId} sprint=${sprintId || 'none'} issue=${issueId || 'none'} cleanup=${createdWorkspace || createdProject}`,
  );

  return {
    baseUrl: cfg.baseUrl,
    token,
    workspaceId,
    projectId,
    issueId,
    sprintId,
    createdWorkspace,
    createdProject,
    ...refs,
  };
}

/**
 * Deletes ephemeral workspace/project created by setup (CASCADE removes issues/sprints).
 * Set K6_SKIP_CLEANUP=true to keep data for debugging.
 */
export function cleanupTestData(data) {
  if (!data || __ENV.K6_SKIP_CLEANUP === 'true' || __ENV.K6_SKIP_CLEANUP === '1') {
    console.log('teardown: skipped');
    return;
  }

  const headers = authHeaders(data.token);
  const { baseUrl, workspaceId, projectId, createdWorkspace, createdProject } = data;

  // Workspace delete cascades projects+issues. Prefer that when we own the workspace.
  if (createdWorkspace && workspaceId) {
    const res = http.del(`${baseUrl}/api/v1/workspaces/${workspaceId}`, null, {
      headers,
      timeout: '180s',
      tags: { name: 'DELETE /workspaces (teardown)' },
    });
    console.log(
      `teardown workspace ${workspaceId}: status=${res.status} body=${String(res.body).slice(0, 200)}`,
    );
    return;
  }

  if (createdProject && projectId) {
    const res = http.del(`${baseUrl}/api/v1/projects/${projectId}`, null, {
      headers,
      timeout: '180s',
      tags: { name: 'DELETE /projects (teardown)' },
    });
    console.log(
      `teardown project ${projectId}: status=${res.status} body=${String(res.body).slice(0, 200)}`,
    );
  }
}

function discoverOrCreateActiveSprint(baseUrl, headers, projectId) {
  const list = http.get(`${baseUrl}/api/v1/projects/${projectId}/sprints`, {
    headers,
    tags: { name: 'GET /projects/sprints (setup)' },
  });
  checkOk(list, 'setup sprints');

  const sprints = list.json('data') || [];
  const active = sprints.find((s) => s.status === 'ACTIVE');
  if (active?.id) {
    return active.id;
  }

  const planned = sprints.find((s) => s.status === 'PLANNED');
  let sprintId = planned?.id;

  if (!sprintId) {
    const create = http.post(
      `${baseUrl}/api/v1/projects/${projectId}/sprints`,
      JSON.stringify({
        name: `k6 Sprint ${Date.now()}`,
        goal: 'Active sprint for write-heavy board reads',
      }),
      { headers, tags: { name: 'POST /projects/sprints (setup)' } },
    );
    checkCreated(create, 'setup create sprint');
    sprintId = create.json('data.id');
  }

  if (!sprintId) {
    console.warn('setup: could not create sprint; board will return empty columns');
    return null;
  }

  const start = http.post(`${baseUrl}/api/v1/sprints/${sprintId}/start`, null, {
    headers,
    tags: { name: 'POST /sprints/{id}/start (setup)' },
  });
  if (start.status < 200 || start.status >= 300) {
    console.warn(`setup: start sprint failed status=${start.status} body=${String(start.body).slice(0, 200)}`);
  }

  return sprintId;
}
