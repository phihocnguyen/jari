-- V11: Add position column to issues table for backlog and project issue ordering
ALTER TABLE issues ADD COLUMN IF NOT EXISTS position NUMERIC(20,6) NOT NULL DEFAULT 1000;
CREATE INDEX IF NOT EXISTS idx_issues_project_position ON issues(project_id, position);
