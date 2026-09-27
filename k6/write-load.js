import { setupTestData, cleanupTestData } from './lib/setup.js';
import { writeHeavy } from './scenarios/write-heavy.js';
import http from 'k6/http';
import { authHeaders, checkOk } from './lib/http.js';

function intEnv(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === '') return fallback;
  const parsed = parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

const concurrentUsers = intEnv('K6_CONCURRENT_USERS', 100);
const duration = __ENV.K6_LOAD_DURATION || '5m';
// Small fixed pool — all updates hammer these issues (not growing board via sprint)
const seedIssues = intEnv('K6_SEED_ISSUES', 5);

export const options = {
  scenarios: {
    write_heavy: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: concurrentUsers },
        { duration, target: concurrentUsers },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
      exec: 'writeHeavy',
      tags: { scenario: 'write_heavy' },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.15'],
    'http_req_failed{name:POST /projects/issues}': ['rate<0.50'],
    'http_req_duration{scenario:write_heavy}': ['p(95)<10000'],
    'http_req_duration{workload:write}': ['p(95)<5000'],
    'http_req_duration{workload:read}': ['p(95)<5000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  const data = setupTestData({ ephemeral: true });
  const headers = authHeaders(data.token);
  const { baseUrl, projectId, issueTypeId, statusId, priorityId } = data;

  const statusesRes = http.get(`${baseUrl}/api/v1/ref/statuses`, {
    headers,
    tags: { name: 'GET /ref/statuses (setup)' },
  });
  checkOk(statusesRes, 'setup statuses');
  const statusIds = (statusesRes.json('data') || []).map((s) => s.id).filter(Boolean);

  // Seed a few backlog issues (no sprint) — hot set for status/field updates
  const issueIds = [];
  if (issueTypeId && statusId && priorityId) {
    for (let i = 0; i < seedIssues; i++) {
      const body = {
        title: `k6-seed-${i}-${Date.now()}`,
        description: 'Seed issue for field-update write-heavy',
        issueTypeId,
        statusId,
        priorityId,
      };
      const res = http.post(
        `${baseUrl}/api/v1/projects/${projectId}/issues`,
        JSON.stringify(body),
        {
          headers,
          tags: { name: 'POST /projects/issues (setup)' },
          timeout: '30s',
        },
      );
      if (res.status === 201) {
        try {
          const id = res.json('data.id');
          if (id) issueIds.push(id);
        } catch {
          // ignore
        }
      }
    }
  }

  if (issueIds.length === 0) {
    console.warn('write-heavy setup: no seed issues');
  } else {
    console.log(
      `write-heavy setup: ${issueIds.length} seed issues (backlog, no sprint), ${statusIds.length} statuses`,
    );
  }

  return {
    ...data,
    statusIds,
    issueIds,
  };
}

export function teardown(data) {
  cleanupTestData(data);
}

export { writeHeavy };
