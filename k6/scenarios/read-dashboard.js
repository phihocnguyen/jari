import http from 'k6/http';
import { sleep } from 'k6';
import { authHeaders, checkOk, checkJson } from '../lib/http.js';

/**
 * Mô phỏng user mở dashboard: ref data → workspaces → projects → issues → detail → summary → board.
 */
export function readDashboard(data) {
  const headers = authHeaders(data.token);
  const { baseUrl, workspaceId, projectId, issueId } = data;

  const refCalls = [
    http.get(`${baseUrl}/api/v1/ref/issue-types`, { headers, tags: { name: 'GET /ref/issue-types' } }),
    http.get(`${baseUrl}/api/v1/ref/statuses`, { headers, tags: { name: 'GET /ref/statuses' } }),
    http.get(`${baseUrl}/api/v1/ref/priorities`, { headers, tags: { name: 'GET /ref/priorities' } }),
  ];
  refCalls.forEach((res, i) => checkOk(res, `ref ${i}`));

  const workspaces = http.get(`${baseUrl}/api/v1/workspaces`, {
    headers,
    tags: { name: 'GET /workspaces' },
  });
  checkJson(workspaces, 'workspaces');

  const projects = http.get(`${baseUrl}/api/v1/workspaces/${workspaceId}/projects`, {
    headers,
    tags: { name: 'GET /workspace/projects' },
  });
  checkJson(projects, 'projects');

  const issues = http.get(`${baseUrl}/api/v1/projects/${projectId}/issues?page=0&size=20`, {
    headers,
    tags: { name: 'GET /projects/issues' },
  });
  checkJson(issues, 'issues list');

  if (issueId) {
    const issue = http.get(`${baseUrl}/api/v1/issues/${issueId}`, {
      headers,
      tags: { name: 'GET /issues/{id}' },
    });
    checkJson(issue, 'issue detail');
  }

  const summary = http.get(`${baseUrl}/api/v1/projects/${projectId}/summary`, {
    headers,
    tags: { name: 'GET /projects/summary' },
  });
  checkOk(summary, 'summary');

  const board = http.get(`${baseUrl}/api/v1/projects/${projectId}/board`, {
    headers,
    tags: { name: 'GET /projects/board' },
  });
  checkOk(board, 'board');

  sleep(Math.random() * 2 + 1);
}
