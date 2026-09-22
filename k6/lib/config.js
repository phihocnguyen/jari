export function getConfig() {
  return {
    baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
    email: __ENV.AUTH_EMAIL || '',
    password: __ENV.AUTH_PASSWORD || '',
    workspaceId: __ENV.WORKSPACE_ID || '',
    projectId: __ENV.PROJECT_ID || '',
    issueId: __ENV.ISSUE_ID || '',
    scenario: __ENV.K6_SCENARIO || 'all',
  };
}
