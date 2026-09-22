import http from 'k6/http';
import { fail } from 'k6';
import { getConfig } from './config.js';
import { authHeaders, checkOk } from './http.js';
import { login } from './auth.js';

function firstId(list) {
  if (!Array.isArray(list) || list.length === 0) {
    return null;
  }
  return list[0].id;
}

function discoverWorkspace(baseUrl, headers, workspaceId) {
  if (workspaceId) {
    return workspaceId;
  }
  const res = http.get(`${baseUrl}/api/v1/workspaces`, {
    headers,
    tags: { name: 'GET /workspaces (setup)' },
  });
  checkOk(res, 'setup workspaces');
  return firstId(res.json('data'));
}

function discoverProject(baseUrl, headers, workspaceId, projectId) {
  if (projectId) {
    return projectId;
  }
  const res = http.get(`${baseUrl}/api/v1/workspaces/${workspaceId}/projects`, {
    headers,
    tags: { name: 'GET /projects (setup)' },
  });
  checkOk(res, 'setup projects');
  return firstId(res.json('data'));
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

export function setupTestData() {
  const cfg = getConfig();
  const token = login();
  const headers = authHeaders(token);

  const health = http.get(`${cfg.baseUrl}/actuator/health`, {
    tags: { name: 'GET /actuator/health (setup)' },
  });
  if (health.status !== 200) {
    fail(`Backend not reachable at ${cfg.baseUrl} (health=${health.status})`);
  }

  const workspaceId = discoverWorkspace(cfg.baseUrl, headers, cfg.workspaceId);
  if (!workspaceId) {
    fail(
      'No workspace found. Create one via API/UI or set WORKSPACE_ID + PROJECT_ID in k6/.env',
    );
  }

  const projectId = discoverProject(cfg.baseUrl, headers, workspaceId, cfg.projectId);
  if (!projectId) {
    fail(
      `No project in workspace ${workspaceId}. Create a project or set PROJECT_ID in k6/.env`,
    );
  }

  const issueId = discoverIssue(cfg.baseUrl, headers, projectId, cfg.issueId);
  const refs = loadReferenceIds(cfg.baseUrl, headers);

  return {
    baseUrl: cfg.baseUrl,
    token,
    workspaceId,
    projectId,
    issueId,
    ...refs,
  };
}
