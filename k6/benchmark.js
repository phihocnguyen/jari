import { setupTestData } from './lib/setup.js';
import { getConfig } from './lib/config.js';
import { smoke } from './scenarios/smoke.js';
import { readDashboard } from './scenarios/read-dashboard.js';
import { mixedWorkload } from './scenarios/mixed-workload.js';

const cfg = getConfig();

// Khi K6_SCENARIO=all: chạy tuần tự (startTime) — tránh 3 scenario cùng bắn 1 lúc
const scenarioDefs = {
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
    exec: 'smoke',
    tags: { scenario: 'smoke' },
    startTime: '0s',
  },
  read_dashboard: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '20s', target: Math.min(3, cfg.readVus) },
      { duration: '1m', target: cfg.readVus },
      { duration: '30s', target: cfg.readVus },
      { duration: '20s', target: 0 },
    ],
    gracefulRampDown: '10s',
    exec: 'readDashboard',
    tags: { scenario: 'read_dashboard' },
    startTime: cfg.scenario === 'all' ? '35s' : '0s',
  },
  mixed_workload: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '20s', target: Math.min(2, cfg.mixedVus) },
      { duration: '1m', target: cfg.mixedVus },
      { duration: '20s', target: 0 },
    ],
    gracefulRampDown: '10s',
    exec: 'mixedWorkload',
    tags: { scenario: 'mixed_workload' },
    // smoke 30s + read ~2m10s
    startTime: cfg.scenario === 'all' ? '3m30s' : '0s',
  },
};

function pickScenarios() {
  if (cfg.scenario === 'all') {
    return scenarioDefs;
  }
  if (!scenarioDefs[cfg.scenario]) {
    throw new Error(
      `Unknown K6_SCENARIO="${cfg.scenario}". Use: smoke | read_dashboard | mixed_workload | all`,
    );
  }
  const selected = { ...scenarioDefs[cfg.scenario] };
  delete selected.startTime;
  return { [cfg.scenario]: selected };
}

const devThresholds = {
  http_req_failed: ['rate<0.05'],
  'http_req_duration{scenario:smoke}': ['p(95)<500'],
  'http_req_duration{scenario:read_dashboard}': ['p(95)<1500', 'p(99)<2500'],
  'http_req_duration{scenario:mixed_workload}': ['p(95)<2000', 'p(99)<3500'],
  'http_req_duration{name:GET /projects/issues}': ['p(95)<1200'],
  'http_req_duration{name:GET /projects/board}': ['p(95)<1500'],
  'http_req_duration{name:POST /projects/issues}': ['p(95)<2000'],
};

const strictThresholds = {
  http_req_failed: ['rate<0.02'],
  'http_req_duration{scenario:smoke}': ['p(95)<500'],
  'http_req_duration{scenario:read_dashboard}': ['p(95)<1500', 'p(99)<3000'],
  'http_req_duration{scenario:mixed_workload}': ['p(95)<2000', 'p(99)<4000'],
  'http_req_duration{name:GET /projects/issues}': ['p(95)<1200'],
  'http_req_duration{name:GET /projects/board}': ['p(95)<1500'],
  'http_req_duration{name:POST /projects/issues}': ['p(95)<2500'],
};

export const options = {
  scenarios: pickScenarios(),
  thresholds: cfg.strictThresholds ? strictThresholds : devThresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return setupTestData();
}

export { smoke, readDashboard, mixedWorkload };
