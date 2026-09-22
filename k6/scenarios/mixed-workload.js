import http from 'k6/http';
import { sleep } from 'k6';
import { getConfig } from '../lib/config.js';
import { authHeaders, checkCreated } from '../lib/http.js';
import { readDashboard } from './read-dashboard.js';

/**
 * Chủ yếu read; tỉ lệ write cấu hình qua K6_WRITE_RATIO (mặc định 10%).
 */
export function mixedWorkload(data) {
  const { writeRatio } = getConfig();

  if (Math.random() >= writeRatio) {
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
    timeout: '30s',
  });
  checkCreated(create, 'create issue');

  sleep(Math.random() + 0.5);
}
