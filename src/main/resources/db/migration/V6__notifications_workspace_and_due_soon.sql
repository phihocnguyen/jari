-- Workspace context on notifications + ISSUE_DUE_SOON type
ALTER TABLE notifications ADD COLUMN workspace_id UUID;
ALTER TABLE notifications ADD COLUMN workspace_name VARCHAR(100);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type CHECK (type IN (
    'ISSUE_ASSIGNED', 'ISSUE_UPDATED', 'ISSUE_COMMENTED',
    'SPRINT_STARTED', 'SPRINT_COMPLETED', 'MEMBER_INVITED', 'ISSUE_DUE_SOON'
));
