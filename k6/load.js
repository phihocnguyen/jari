import { setupTestData } from './lib/setup.js';
import { readDashboardStress } from './scenarios/read-dashboard-stress.js';
import { singleRequest } from './scenarios/single-request.js';

function intEnv(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === '') return fallback;
  const parsed = parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

const concurrentUsers = intEnv('K6_CONCURRENT_USERS', 100);
const targetRps = intEnv('K6_TARGET_RPS', 500);
const maxVUs = intEnv('K6_MAX_VUS', 250);
const duration = __ENV.K6_LOAD_DURATION || '3m';
// vus = 100 concurrent user (full dashboard flow)
// rps = 500 HTTP req/s (1 request/iteration)
// both = chạy tuần tự vus rồi rps
const mode = __ENV.K6_LOAD_MODE || 'both';

const scenarios = {};

if (mode === 'vus' || mode === 'both') {
  scenarios.concurrent_users = {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: concurrentUsers },
      { duration, target: concurrentUsers },
      { duration: '30s', target: 0 },
    ],
    gracefulRampDown: '30s',
    exec: 'readDashboardStress',
    tags: { scenario: 'concurrent_users' },
    startTime: '0s',
  };
}

if (mode === 'rps' || mode === 'both') {
  // Ramp 0 → target RPS trong 1 phút, giữ target, ramp down
  scenarios.target_rps = {
    executor: 'ramping-arrival-rate',
    startRate: 0,
    timeUnit: '1s',
    preAllocatedVUs: concurrentUsers,
    maxVUs,
    stages: [
      { duration: '1m', target: Math.max(1, Math.floor(targetRps / 5)) },
      { duration, target: targetRps },
      { duration: '30s', target: 0 },
    ],
    exec: 'singleRequest',
    tags: { scenario: 'target_rps' },
    startTime: mode === 'both' ? '5m30s' : '0s',
  };
}

export const options = {
  scenarios,
  thresholds: {
    // Stress test: ghi nhận kết quả, không fail sớm
    http_req_failed: ['rate<0.15'],
    'http_req_duration{scenario:concurrent_users}': ['p(95)<10000'],
    'http_req_duration{scenario:target_rps}': ['p(95)<10000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return setupTestData();
}

export { readDashboardStress, singleRequest };
