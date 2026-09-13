-- =========================================================
-- V2: SEED REFERENCE DATA
-- =========================================================

-- =========================================================
-- ROLES
-- =========================================================
INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'WORKSPACE_ADMIN'),
    (gen_random_uuid(), 'WORKSPACE_MEMBER'),
    (gen_random_uuid(), 'WORKSPACE_VIEWER'),
    (gen_random_uuid(), 'PROJECT_ADMIN'),
    (gen_random_uuid(), 'PROJECT_MEMBER'),
    (gen_random_uuid(), 'PROJECT_VIEWER');

-- =========================================================
-- PERMISSIONS
-- =========================================================
INSERT INTO permissions (id, name, description) VALUES
    -- Workspace permissions
    (gen_random_uuid(), 'WORKSPACE_READ',          'View workspace details'),
    (gen_random_uuid(), 'WORKSPACE_UPDATE',        'Update workspace settings'),
    (gen_random_uuid(), 'WORKSPACE_DELETE',        'Delete workspace'),
    (gen_random_uuid(), 'WORKSPACE_MEMBER_MANAGE', 'Invite/remove workspace members'),
    -- Project permissions
    (gen_random_uuid(), 'PROJECT_CREATE',          'Create projects in workspace'),
    (gen_random_uuid(), 'PROJECT_READ',            'View project details'),
    (gen_random_uuid(), 'PROJECT_UPDATE',          'Update project settings'),
    (gen_random_uuid(), 'PROJECT_DELETE',          'Delete/archive project'),
    (gen_random_uuid(), 'PROJECT_MEMBER_MANAGE',   'Manage project members'),
    -- Issue permissions
    (gen_random_uuid(), 'ISSUE_CREATE',            'Create issues'),
    (gen_random_uuid(), 'ISSUE_READ',              'View issues'),
    (gen_random_uuid(), 'ISSUE_UPDATE',            'Update issue fields'),
    (gen_random_uuid(), 'ISSUE_DELETE',            'Delete issues'),
    (gen_random_uuid(), 'ISSUE_ASSIGN',            'Assign issues to users'),
    -- Sprint permissions
    (gen_random_uuid(), 'SPRINT_CREATE',           'Create sprints'),
    (gen_random_uuid(), 'SPRINT_MANAGE',           'Start/complete sprints'),
    -- Comment permissions
    (gen_random_uuid(), 'COMMENT_CREATE',          'Add comments'),
    (gen_random_uuid(), 'COMMENT_DELETE',          'Delete any comment');

-- =========================================================
-- ISSUE TYPES
-- =========================================================
INSERT INTO issue_types (id, name, description) VALUES
    (gen_random_uuid(), 'EPIC',    'Large body of work that can be broken down'),
    (gen_random_uuid(), 'STORY',   'User story or feature request'),
    (gen_random_uuid(), 'TASK',    'A task to be completed'),
    (gen_random_uuid(), 'BUG',     'A defect or problem found in the product'),
    (gen_random_uuid(), 'SUBTASK', 'A subtask belonging to a parent issue');

-- =========================================================
-- STATUSES
-- =========================================================
INSERT INTO statuses (id, name, category) VALUES
    (gen_random_uuid(), 'TO DO',       'TODO'),
    (gen_random_uuid(), 'IN PROGRESS', 'IN_PROGRESS'),
    (gen_random_uuid(), 'IN REVIEW',   'IN_PROGRESS'),
    (gen_random_uuid(), 'DONE',        'DONE'),
    (gen_random_uuid(), 'CANCELLED',   'DONE');

-- =========================================================
-- PRIORITIES
-- =========================================================
INSERT INTO priorities (id, name, level) VALUES
    (gen_random_uuid(), 'HIGHEST', 1),
    (gen_random_uuid(), 'HIGH',    2),
    (gen_random_uuid(), 'MEDIUM',  3),
    (gen_random_uuid(), 'LOW',     4),
    (gen_random_uuid(), 'LOWEST',  5);
