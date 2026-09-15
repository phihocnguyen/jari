-- =========================================================
-- V1: INITIAL SCHEMA
-- PostgreSQL · Jari (Jira Clone)
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================================
-- 1. USERS
-- =========================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name  VARCHAR(100) NOT NULL,
    avatar_url    TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- =========================================================
-- 2. OAUTH ACCOUNTS
-- =========================================================
CREATE TABLE oauth_accounts (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL,
    provider         VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_accounts_user     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_oauth_provider_user     UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_oauth_accounts_user ON oauth_accounts(user_id);

-- =========================================================
-- 3. ROLES
-- =========================================================
CREATE TABLE roles (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 4. PERMISSIONS
-- =========================================================
CREATE TABLE permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- =========================================================
-- 5. ROLE PERMISSIONS
-- =========================================================
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- =========================================================
-- 6. WORKSPACES
-- =========================================================
CREATE TABLE workspaces (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    workspace_key VARCHAR(20)  NOT NULL UNIQUE,
    description   TEXT,
    owner_id      UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);

-- =========================================================
-- 7. WORKSPACE MEMBERS
-- =========================================================
CREATE TABLE workspace_members (
    workspace_id UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    role_id      UUID        NOT NULL,
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user      FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_role      FOREIGN KEY (role_id)      REFERENCES roles(id)      ON DELETE RESTRICT
);
CREATE INDEX idx_workspace_members_user ON workspace_members(user_id);

-- =========================================================
-- 8. PROJECTS
-- =========================================================
CREATE TABLE projects (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    project_key  VARCHAR(20)  NOT NULL,
    description  TEXT,
    lead_id      UUID,
    project_type VARCHAR(30)  NOT NULL DEFAULT 'SOFTWARE',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_workspace    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_projects_lead         FOREIGN KEY (lead_id)      REFERENCES users(id)      ON DELETE SET NULL,
    CONSTRAINT uq_project_key_per_workspace UNIQUE (workspace_id, project_key),
    CONSTRAINT chk_projects_status      CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_projects_type        CHECK (project_type IN ('SOFTWARE', 'BUSINESS', 'SERVICE'))
);
CREATE INDEX idx_projects_workspace ON projects(workspace_id);
CREATE INDEX idx_projects_lead      ON projects(lead_id);

-- =========================================================
-- 9. PROJECT MEMBERS
-- =========================================================
CREATE TABLE project_members (
    project_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role_id    UUID        NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_project_members_role    FOREIGN KEY (role_id)    REFERENCES roles(id)    ON DELETE RESTRICT
);
CREATE INDEX idx_project_members_user ON project_members(user_id);

-- =========================================================
-- 10. ISSUE TYPES
-- =========================================================
CREATE TABLE issue_types (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 11. STATUSES
-- =========================================================
CREATE TABLE statuses (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    category   VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status_category CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

-- =========================================================
-- 12. PRIORITIES
-- =========================================================
CREATE TABLE priorities (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    level      INTEGER     NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_priority_level CHECK (level > 0)
);

-- =========================================================
-- 13. ISSUES
-- =========================================================
CREATE TABLE issues (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID           NOT NULL,
    issue_key     VARCHAR(30)    NOT NULL,
    title         VARCHAR(255)   NOT NULL,
    description   TEXT,
    issue_type_id UUID           NOT NULL,
    status_id     UUID           NOT NULL,
    priority_id   UUID           NOT NULL,
    reporter_id   UUID           NOT NULL,
    assignee_id   UUID,
    parent_id     UUID,
    story_points  NUMERIC(5,2),
    due_date      DATE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issues_project  FOREIGN KEY (project_id)    REFERENCES projects(id)    ON DELETE CASCADE,
    CONSTRAINT fk_issues_type     FOREIGN KEY (issue_type_id) REFERENCES issue_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_issues_status   FOREIGN KEY (status_id)     REFERENCES statuses(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_issues_priority FOREIGN KEY (priority_id)   REFERENCES priorities(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id)   REFERENCES users(id)       ON DELETE RESTRICT,
    CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id)   REFERENCES users(id)       ON DELETE SET NULL,
    CONSTRAINT fk_issues_parent   FOREIGN KEY (parent_id)     REFERENCES issues(id)      ON DELETE SET NULL,
    CONSTRAINT uq_issue_key_per_project  UNIQUE (project_id, issue_key),
    CONSTRAINT chk_issue_story_points    CHECK (story_points IS NULL OR story_points >= 0)
);
CREATE INDEX idx_issues_project        ON issues(project_id);
CREATE INDEX idx_issues_status         ON issues(status_id);
CREATE INDEX idx_issues_assignee       ON issues(assignee_id);
CREATE INDEX idx_issues_reporter       ON issues(reporter_id);
CREATE INDEX idx_issues_parent         ON issues(parent_id);
CREATE INDEX idx_issues_project_status ON issues(project_id, status_id);

-- =========================================================
-- 14. SPRINTS
-- =========================================================
CREATE TABLE sprints (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    goal       TEXT,
    start_date TIMESTAMPTZ,
    end_date   TIMESTAMPTZ,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sprints_project  FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_sprint_status   CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_sprint_dates    CHECK (end_date IS NULL OR start_date IS NULL OR end_date > start_date)
);
CREATE INDEX idx_sprints_project        ON sprints(project_id);
CREATE INDEX idx_sprints_project_status ON sprints(project_id, status);

-- =========================================================
-- 15. SPRINT ISSUES
-- =========================================================
CREATE TABLE sprint_issues (
    sprint_id UUID           NOT NULL,
    issue_id  UUID           NOT NULL,
    position  NUMERIC(20,6)  NOT NULL DEFAULT 0,
    added_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sprint_id, issue_id),
    CONSTRAINT fk_sprint_issues_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    CONSTRAINT fk_sprint_issues_issue  FOREIGN KEY (issue_id)  REFERENCES issues(id)  ON DELETE CASCADE
);
CREATE INDEX idx_sprint_issues_issue    ON sprint_issues(issue_id);
CREATE INDEX idx_sprint_issues_position ON sprint_issues(sprint_id, position);

-- =========================================================
-- 16. COMMENTS
-- =========================================================
CREATE TABLE comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id   UUID        NOT NULL,
    author_id  UUID        NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_comments_issue  FOREIGN KEY (issue_id)  REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id)  ON DELETE RESTRICT
);
CREATE INDEX idx_comments_issue  ON comments(issue_id);
CREATE INDEX idx_comments_author ON comments(author_id);

-- =========================================================
-- 17. ISSUE HISTORY
-- =========================================================
CREATE TABLE issue_history (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    field      VARCHAR(50) NOT NULL,
    old_value  TEXT,
    new_value  TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_history_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_history_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE RESTRICT
);
CREATE INDEX idx_issue_history_issue ON issue_history(issue_id, created_at DESC);
CREATE INDEX idx_issue_history_user  ON issue_history(user_id);
