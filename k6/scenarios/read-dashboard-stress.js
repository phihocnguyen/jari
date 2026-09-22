import http from 'k6/http';
import { authHeaders, checkOk, checkJson } from '../lib/http.js';

/** Giống read-dashboard nhưng không sleep — dùng stress test 100 VU. */
export function readDashboardStress(data) {
  const headers = authHeaders(data.token);
  const { baseUrl, workspaceId, projectId, issueId } = data;

  [
    http.get(`${baseUrl}/api/v1/ref/issue-types`, { headers, tags: { name: 'GET /ref/issue-types' } }),
    http.get(`${baseUrl}/api/v1/ref/statuses`, { headers, tags: { name: 'GET /ref/statuses' } }),
    http.get(`${baseUrl}/api/v1/ref/priorities`, { headers, tags: { name: 'GET /ref/priorities' } }),
  ].forEach((res, i) => checkOk(res, `ref ${i}`));

  checkJson(http.get(`${baseUrl}/api/v1/workspaces`, { headers, tags: { name: 'GET /workspaces' } }), 'workspaces');
  checkJson(
    http.get(`${baseUrl}/api/v1/workspaces/${workspaceId}/projects`, { headers, tags: { name: 'GET /workspace/projects' } }),
    'projects',
  );
  checkJson(
    http.get(`${baseUrl}/api/v1/projects/${projectId}/issues?page=0&size=20`, { headers, tags: { name: 'GET /projects/issues' } }),
    'issues list',
  );

  if (issueId) {
    checkJson(http.get(`${baseUrl}/api/v1/issues/${issueId}`, { headers, tags: { name: 'GET /issues/{id}' } }), 'issue detail');
  }

  checkOk(http.get(`${baseUrl}/api/v1/projects/${projectId}/summary`, { headers, tags: { name: 'GET /projects/summary' } }), 'summary');
  checkOk(http.get(`${baseUrl}/api/v1/projects/${projectId}/board`, { headers, tags: { name: 'GET /projects/board' } }), 'board');
}
