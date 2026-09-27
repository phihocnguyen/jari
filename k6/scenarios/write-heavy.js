import http from 'k6/http';
import { sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { authHeaders, checkCreated, checkOk, checkJson } from '../lib/http.js';

// Separate counters (no tags) so they always appear distinctly in the k6 summary
export const create201 = new Counter('create_http_201');
export const create500 = new Counter('create_http_500');
export const create503 = new Counter('create_http_503');
export const create409 = new Counter('create_http_409');
export const create0 = new Counter('create_http_0');
export const createOther = new Counter('create_http_other');
export const endpointFail = new Counter('endpoint_fail');

function thinkSeconds() {
  const ms = parseInt(__ENV.K6_THINK_MS || '300', 10);
  if (Number.isNaN(ms) || ms <= 0) return 0;
  return ms / 1000;
}

/**
 * Field-update focused workload on a small fixed issue pool (setup seeds).
 * Creates are backlog-only (no sprint) so the board stays small.
 *
 * Mix: ~10% CREATE / ~70% UPDATE (status + fields) / ~20% READ
 */
export function writeHeavy(data) {
  const headers = authHeaders(data.token);
  const { baseUrl, projectId, issueTypeId, statusId, priorityId, statusIds, issueIds } = data;

  const roll = Math.random();

  if (roll < 0.1) {
    createIssue(baseUrl, headers, projectId, issueTypeId, statusId, priorityId);
  } else if (roll < 0.8) {
    updateIssue(baseUrl, headers, data, statusIds, issueIds);
  } else {
    readAfterWrite(baseUrl, headers, projectId, issueIds);
  }

  const think = thinkSeconds();
  if (think > 0) {
    sleep(think);
  }
}

function trackFail(res, name) {
  if (res.status < 200 || res.status >= 300) {
    endpointFail.add(1, { name, status: String(res.status) });
  }
}

function trackCreateStatus(res) {
  if (res.status === 201) create201.add(1);
  else if (res.status === 500) create500.add(1);
  else if (res.status === 503) create503.add(1);
  else if (res.status === 409) create409.add(1);
  else if (res.status === 0) create0.add(1);
  else createOther.add(1);
}

/** Backlog create — never attach sprint (keeps board stable for latency measurement). */
function createIssue(baseUrl, headers, projectId, issueTypeId, statusId, priorityId) {
  if (!issueTypeId || !statusId || !priorityId) {
    return;
  }

  const body = {
    title: `k6-wh vu${__VU}-i${__ITER}-${Date.now()}`,
    description: 'Created by k6 write-heavy scenario (backlog)',
    issueTypeId,
    statusId,
    priorityId,
  };

  const res = http.post(`${baseUrl}/api/v1/projects/${projectId}/issues`, JSON.stringify(body), {
    headers,
    tags: { name: 'POST /projects/issues', workload: 'write' },
    timeout: '30s',
  });
  trackCreateStatus(res);
  trackFail(res, 'POST /projects/issues');
  checkCreated(res, 'create issue');

  if (res.status !== 201 && !globalThis.__loggedCreateFail) {
    globalThis.__loggedCreateFail = true;
    console.warn(
      `create fail sample: status=${res.status} body=${String(res.body).slice(0, 500)}`,
    );
  }
}

/** Always pick from the shared seed pool (few issues), not a growing per-VU list. */
function pickSeedIssueId(dataIssueIds) {
  if (!Array.isArray(dataIssueIds) || dataIssueIds.length === 0) {
    return null;
  }
  return dataIssueIds[Math.floor(Math.random() * dataIssueIds.length)];
}

function updateIssue(baseUrl, headers, data, statusIds, issueIds) {
  const issueId = pickSeedIssueId(issueIds);
  if (!issueId) {
    createIssue(
      baseUrl,
      headers,
      data.projectId,
      data.issueTypeId,
      data.statusId,
      data.priorityId,
    );
    return;
  }

  const roll = Math.random();

  // ~50% of updates: status transition
  if (roll < 0.5 && Array.isArray(statusIds) && statusIds.length > 0) {
    const nextStatus = statusIds[Math.floor(Math.random() * statusIds.length)];
    const res = http.patch(
      `${baseUrl}/api/v1/issues/${issueId}/status`,
      JSON.stringify({ statusId: nextStatus }),
      {
        headers,
        tags: { name: 'PATCH /issues/{id}/status', workload: 'write' },
        timeout: '30s',
      },
    );
    trackFail(res, 'PATCH /issues/{id}/status');
    checkOk(res, 'update status');
    return;
  }

  // ~30%: title + description
  if (roll < 0.8) {
    const res = http.put(
      `${baseUrl}/api/v1/issues/${issueId}`,
      JSON.stringify({
        title: `k6-upd vu${__VU}-i${__ITER}-${Date.now()}`,
        description: `Updated by k6 write-heavy vu=${__VU} iter=${__ITER}`,
      }),
      {
        headers,
        tags: { name: 'PUT /issues/{id}', workload: 'write' },
        timeout: '30s',
      },
    );
    trackFail(res, 'PUT /issues/{id}');
    checkOk(res, 'update issue');
    return;
  }

  // ~20%: priority field (same PUT endpoint)
  const priorityId = data.priorityId;
  const res = http.put(
    `${baseUrl}/api/v1/issues/${issueId}`,
    JSON.stringify(priorityId ? { priorityId } : { title: `k6-prio ${Date.now()}` }),
    {
      headers,
      tags: { name: 'PUT /issues/{id}', workload: 'write' },
      timeout: '30s',
    },
  );
  trackFail(res, 'PUT /issues/{id}');
  checkOk(res, 'update issue');
}

function readAfterWrite(baseUrl, headers, projectId, issueIds) {
  const r = Math.random();

  // Prefer detail of the hot seed issues (realistic after-write read)
  if (r < 0.5) {
    const issueId = pickSeedIssueId(issueIds);
    if (issueId) {
      const res = http.get(`${baseUrl}/api/v1/issues/${issueId}`, {
        headers,
        tags: { name: 'GET /issues/{id}', workload: 'read' },
      });
      trackFail(res, 'GET /issues/{id}');
      checkJson(res, 'issue detail');
      return;
    }
  }

  if (r < 0.85) {
    const res = http.get(`${baseUrl}/api/v1/projects/${projectId}/issues?page=0&size=20`, {
      headers,
      tags: { name: 'GET /projects/issues', workload: 'read' },
    });
    trackFail(res, 'GET /projects/issues');
    checkJson(res, 'issues list');
    return;
  }

  // Occasional board — stays small because creates are backlog-only
  const res = http.get(`${baseUrl}/api/v1/projects/${projectId}/board`, {
    headers,
    tags: { name: 'GET /projects/board', workload: 'read' },
  });
  trackFail(res, 'GET /projects/board');
  checkOk(res, 'board');
}
