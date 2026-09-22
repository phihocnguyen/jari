function intEnv(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === '') {
    return fallback;
  }
  const parsed = parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function getConfig() {
  return {
    baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
    email: __ENV.AUTH_EMAIL || '',
    password: __ENV.AUTH_PASSWORD || '',
    workspaceId: __ENV.WORKSPACE_ID || '',
    projectId: __ENV.PROJECT_ID || '',
    issueId: __ENV.ISSUE_ID || '',
    // all = chạy tuần tự smoke → read → mixed | hoặc chọn 1 scenario
    scenario: __ENV.K6_SCENARIO || 'all',
    readVus: intEnv('K6_READ_VUS', 10),
    mixedVus: intEnv('K6_MIXED_VUS', 5),
    writeRatio: intEnv('K6_WRITE_RATIO', 10) / 100,
    strictThresholds: __ENV.K6_STRICT_THRESHOLDS === 'true',
  };
}
