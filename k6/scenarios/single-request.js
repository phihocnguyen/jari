import http from 'k6/http';
import { authHeaders, checkOk } from '../lib/http.js';

/**
 * 1 iteration = 1 HTTP request. Dùng với constant-arrival-rate để target RPS chính xác.
 */
export function singleRequest(data) {
  const headers = authHeaders(data.token);
  const { baseUrl, workspaceId, projectId, issueId } = data;

  const roll = Math.random();
  let res;

  if (roll < 0.35) {
    res = http.get(`${baseUrl}/api/v1/projects/${projectId}/issues?page=0&size=20`, {
      headers,
      tags: { name: 'GET /projects/issues' },
    });
  } else if (roll < 0.55) {
    res = http.get(`${baseUrl}/api/v1/projects/${projectId}/board`, {
      headers,
      tags: { name: 'GET /projects/board' },
    });
  } else if (roll < 0.70) {
    res = http.get(`${baseUrl}/api/v1/projects/${projectId}/summary`, {
      headers,
      tags: { name: 'GET /projects/summary' },
    });
  } else if (roll < 0.82) {
    res = http.get(`${baseUrl}/api/v1/workspaces/${workspaceId}/projects`, {
      headers,
      tags: { name: 'GET /workspace/projects' },
    });
  } else if (roll < 0.92 && issueId) {
    res = http.get(`${baseUrl}/api/v1/issues/${issueId}`, {
      headers,
      tags: { name: 'GET /issues/{id}' },
    });
  } else if (roll < 0.97) {
    res = http.get(`${baseUrl}/api/v1/ref/statuses`, {
      headers,
      tags: { name: 'GET /ref/statuses' },
    });
  } else {
    res = http.get(`${baseUrl}/actuator/health`, {
      tags: { name: 'GET /actuator/health' },
    });
  }

  checkOk(res, 'request');
}
