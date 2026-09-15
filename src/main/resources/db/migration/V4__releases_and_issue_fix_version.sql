-- Releases (fix versions)
CREATE TABLE releases (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'UNRELEASED',
    release_date DATE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_releases_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_release_name_per_project UNIQUE (project_id, name),
    CONSTRAINT chk_release_status CHECK (status IN ('UNRELEASED', 'RELEASED', 'ARCHIVED'))
);
CREATE INDEX idx_releases_project ON releases(project_id);

-- Issue → Release (fix version)
ALTER TABLE issues ADD COLUMN release_id UUID;
ALTER TABLE issues ADD CONSTRAINT fk_issues_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE SET NULL;
CREATE INDEX idx_issues_release ON issues(release_id);
