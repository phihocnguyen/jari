import http from 'k6/http';
import { sleep } from 'k6';
import { authHeaders, checkJson } from '../lib/http.js';
import { readDashboard } from './read-dashboard.js';

/**
 * ~80% read (dashboard flow), ~20% tạo issue mới.
 */
export function mixedWorkload(data) {
  if (Math.random() < 0.8) {
    readDashboard(data);
    return;
  }

  const headers = authHeaders(data.token);
  const { baseUrl, projectId, issueTypeId, statusId, priorityId } = data;

  if (!issueTypeId || !statusId || !priorityId) {
    readDashboard(data);
    return;
  }

  const payload = JSON.stringify({
    title: `k6 issue vu${__VU}-iter${__ITER}-${Date.now()}`,
    description: 'Created by k6 mixed workload scenario',
    issueTypeId,
    statusId,
    priorityId,
  });

  const create = http.post(`${baseUrl}/api/v1/projects/${projectId}/issues`, payload, {
    headers,
    tags: { name: 'POST /projects/issues' },
  });
  checkJson(create, 'create issue');

  sleep(Math.random() * 1.5 + 0.5);
}
