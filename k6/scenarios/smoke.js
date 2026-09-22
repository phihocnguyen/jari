import http from 'k6/http';
import { sleep } from 'k6';
import { authHeaders, checkOk } from '../lib/http.js';

export function smoke(data) {
  const headers = authHeaders(data.token);

  const health = http.get(`${data.baseUrl}/actuator/health`, {
    tags: { name: 'GET /actuator/health' },
  });
  checkOk(health, 'health');

  const ref = http.get(`${data.baseUrl}/api/v1/ref/statuses`, {
    headers,
    tags: { name: 'GET /ref/statuses' },
  });
  checkOk(ref, 'ref statuses');

  sleep(1);
}
