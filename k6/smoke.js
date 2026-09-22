import { setupTestData } from './lib/setup.js';
import { smoke } from './scenarios/smoke.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  return setupTestData();
}

export default smoke;
