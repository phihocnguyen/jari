CREATE TABLE issue_developments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    repo_url VARCHAR(500),
    title VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    status VARCHAR(50),
    author VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_dev_issue_id ON issue_developments(issue_id);

CREATE TABLE issue_automation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    rule_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    description TEXT NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_auto_issue_id ON issue_automation_logs(issue_id);
