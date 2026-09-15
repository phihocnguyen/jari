-- Issue start date
ALTER TABLE issues ADD COLUMN start_date DATE;

-- Project-scoped labels
CREATE TABLE labels (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID         NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    color      VARCHAR(20),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_labels_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_label_name_per_project UNIQUE (project_id, name)
);
CREATE INDEX idx_labels_project ON labels(project_id);

-- Issue ↔ Label association
CREATE TABLE issue_labels (
    issue_id UUID NOT NULL,
    label_id UUID NOT NULL,
    PRIMARY KEY (issue_id, label_id),
    CONSTRAINT fk_issue_labels_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_labels_label FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE
);
CREATE INDEX idx_issue_labels_label ON issue_labels(label_id);
