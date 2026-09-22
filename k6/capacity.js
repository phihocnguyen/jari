import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';
import { setupTestData } from './lib/setup.js';
import { readDashboardStress } from './scenarios/read-dashboard-stress.js';
import { singleRequest } from './scenarios/single-request.js';
import { buildCapacityReport, getVuSteps, getRpsSteps } from './lib/capacity-report.js';

function intEnv(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === '') return fallback;
  const parsed = parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

const stepDuration = __ENV.K6_STEP_DURATION || '90s';
const stepSeconds = intEnv('K6_STEP_SECONDS', 90);
const mode = __ENV.K6_CAPACITY_MODE || 'both'; // vus | rps | both
const maxVUs = intEnv('K6_MAX_VUS', 500);

const vuSteps = getVuSteps();
const rpsSteps = getRpsSteps();

function buildVuScenarios() {
  const scenarios = {};
  vuSteps.forEach((step, index) => {
    scenarios[`vu_step_${step.value}`] = {
      executor: 'constant-vus',
      vus: step.value,
      duration: stepDuration,
      exec: 'readDashboardStress',
      tags: { scenario: step.tag },
      startTime: `${index * stepSeconds}s`,
    };
  });
  return scenarios;
}

function buildRpsScenarios() {
  const scenarios = {};
  const rpsStartOffset = mode === 'both' ? vuSteps.length * stepSeconds : 0;

  rpsSteps.forEach((step, index) => {
    scenarios[`rps_step_${step.value}`] = {
      executor: 'constant-arrival-rate',
      rate: step.value,
      timeUnit: '1s',
      duration: stepDuration,
      preAllocatedVUs: Math.min(50, Math.ceil(step.value / 5)),
      maxVUs,
      exec: 'singleRequest',
      tags: { scenario: step.tag },
      startTime: `${rpsStartOffset + index * stepSeconds}s`,
    };
  });
  return scenarios;
}

const scenarios = {};
if (mode === 'vus' || mode === 'both') {
  Object.assign(scenarios, buildVuScenarios());
}
if (mode === 'rps' || mode === 'both') {
  Object.assign(scenarios, buildRpsScenarios());
}

export const options = {
  scenarios,
  // Không fail sớm — chạy hết các step để tìm max
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.99', abortOnFail: false }],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return setupTestData();
}

export function handleSummary(data) {
  const reports = [];
  if (mode === 'vus' || mode === 'both') {
    reports.push(buildCapacityReport(data, 'vus', vuSteps));
  }
  if (mode === 'rps' || mode === 'both') {
    reports.push(buildCapacityReport(data, 'rps', rpsSteps));
  }

  const report = reports.join('\n');
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }) + report,
  };
}

export { readDashboardStress, singleRequest };
