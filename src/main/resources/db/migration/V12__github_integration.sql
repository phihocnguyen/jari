-- GitHub App installations linked to workspaces
CREATE TABLE github_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    installation_id BIGINT NOT NULL,
    account_login VARCHAR(255) NOT NULL,
    account_type VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_github_installations_gh_id UNIQUE (installation_id),
    CONSTRAINT uq_github_installations_workspace UNIQUE (workspace_id, installation_id)
);

CREATE INDEX idx_github_installations_workspace ON github_installations(workspace_id);

-- Repos under an installation; optionally mapped to a Jari project
CREATE TABLE github_repos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installation_id UUID NOT NULL REFERENCES github_installations(id) ON DELETE CASCADE,
    github_repo_id BIGINT NOT NULL,
    full_name VARCHAR(500) NOT NULL,
    html_url VARCHAR(500),
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_github_repos_install_repo UNIQUE (installation_id, github_repo_id)
);

CREATE INDEX idx_github_repos_project ON github_repos(project_id);
CREATE INDEX idx_github_repos_gh_id ON github_repos(github_repo_id);

-- Idempotent upsert key for webhook-linked developments
ALTER TABLE issue_developments
    ADD COLUMN external_id VARCHAR(255),
    ADD COLUMN github_repo_id BIGINT;

CREATE UNIQUE INDEX uq_issue_dev_external
    ON issue_developments (issue_id, type, external_id, github_repo_id)
    WHERE external_id IS NOT NULL AND github_repo_id IS NOT NULL;
