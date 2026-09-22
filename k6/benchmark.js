import { setupTestData } from './lib/setup.js';
import { getConfig } from './lib/config.js';
import { smoke } from './scenarios/smoke.js';
import { readDashboard } from './scenarios/read-dashboard.js';
import { mixedWorkload } from './scenarios/mixed-workload.js';

const cfg = getConfig();

const allScenarios = {
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
    exec: 'smoke',
    tags: { scenario: 'smoke' },
  },
  read_dashboard: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 5 },
      { duration: '2m', target: 20 },
      { duration: '30s', target: 20 },
      { duration: '30s', target: 0 },
    ],
    gracefulRampDown: '15s',
    exec: 'readDashboard',
    tags: { scenario: 'read_dashboard' },
  },
  mixed_workload: {
    executor: 'ramping-arrival-rate',
    startRate: 0,
    timeUnit: '1s',
    preAllocatedVUs: 10,
    maxVUs: 50,
    stages: [
      { duration: '30s', target: 5 },
      { duration: '2m', target: 15 },
      { duration: '30s', target: 0 },
    ],
    exec: 'mixedWorkload',
    tags: { scenario: 'mixed_workload' },
  },
};

function pickScenarios() {
  if (cfg.scenario === 'all') {
    return allScenarios;
  }
  if (!allScenarios[cfg.scenario]) {
    throw new Error(
      `Unknown K6_SCENARIO="${cfg.scenario}". Use: smoke | read_dashboard | mixed_workload | all`,
    );
  }
  return { [cfg.scenario]: allScenarios[cfg.scenario] };
}

export const options = {
  scenarios: pickScenarios(),
  thresholds: {
    http_req_failed: ['rate<0.02'],
    'http_req_duration{scenario:smoke}': ['p(95)<500'],
    'http_req_duration{scenario:read_dashboard}': ['p(95)<1500', 'p(99)<3000'],
    'http_req_duration{scenario:mixed_workload}': ['p(95)<2000', 'p(99)<4000'],
    'http_req_duration{name:GET /projects/issues}': ['p(95)<1200'],
    'http_req_duration{name:GET /projects/board}': ['p(95)<1500'],
    'http_req_duration{name:POST /projects/issues}': ['p(95)<2500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return setupTestData();
}

export { smoke, readDashboard, mixedWorkload };
