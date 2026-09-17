-- Notifications (real-time push + inbox)
CREATE TABLE notifications (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID          NOT NULL,
    type         VARCHAR(50)   NOT NULL,
    message      VARCHAR(500)  NOT NULL,
    issue_id     UUID,
    issue_key    VARCHAR(30),
    project_id   UUID,
    project_name VARCHAR(100),
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_issue     FOREIGN KEY (issue_id)     REFERENCES issues(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_project   FOREIGN KEY (project_id)   REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT chk_notifications_type CHECK (type IN (
        'ISSUE_ASSIGNED', 'ISSUE_UPDATED', 'ISSUE_COMMENTED',
        'SPRINT_STARTED', 'SPRINT_COMPLETED', 'MEMBER_INVITED'
    ))
);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, created_at DESC);
