-- Atomic per-project issue key allocation (fixes MAX+1 race under concurrent creates).
CREATE TABLE project_issue_counter (
    project_id   UUID PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    next_number  BIGINT NOT NULL
);

-- Seed from existing issues (last used number per project).
INSERT INTO project_issue_counter (project_id, next_number)
SELECT p.id,
       COALESCE(
           MAX(
               CASE
                   WHEN i.issue_key ~ ('^' || p.project_key || '-[0-9]+$')
                   THEN CAST(SUBSTRING(i.issue_key FROM LENGTH(p.project_key) + 2) AS INTEGER)
                   ELSE NULL
               END
           ),
           0
       )
FROM projects p
LEFT JOIN issues i ON i.project_id = p.id
GROUP BY p.id;
