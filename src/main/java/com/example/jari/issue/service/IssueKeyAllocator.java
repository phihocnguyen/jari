package com.example.jari.issue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Allocates the next issue number via a single-row {@code UPDATE ... RETURNING}.
 * Uses {@code REQUIRES_NEW} so the counter-row lock is released as soon as the number
 * is assigned — inserts for the same project can then proceed in parallel. Gaps are
 * acceptable if the subsequent insert rolls back (same trade-off as Jira-style keys).
 * <p>
 * Call {@link #allocateNext} <em>before</em> opening the insert transaction so this
 * short TX never nests under a long-lived outer connection (avoids pool deadlock).
 */
@Service
@RequiredArgsConstructor
public class IssueKeyAllocator {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int allocateNext(UUID projectId) {
        Integer next = updateReturning(projectId);
        if (next == null) {
            jdbcTemplate.update("""
                INSERT INTO project_issue_counter (project_id, next_number)
                VALUES (?, 0)
                ON CONFLICT (project_id) DO NOTHING
                """, projectId);
            next = updateReturning(projectId);
        }
        if (next == null) {
            throw new IllegalStateException("Failed to allocate issue number for project " + projectId);
        }
        return next;
    }

    /**
     * Ensures a counter row exists (idempotent). Must join the caller's transaction when
     * invoked from project create — {@code REQUIRES_NEW} cannot see an uncommitted project
     * row and fails the FK to {@code projects}.
     */
    @Transactional
    public void ensureCounterRow(UUID projectId) {
        jdbcTemplate.update("""
            INSERT INTO project_issue_counter (project_id, next_number)
            VALUES (?, 0)
            ON CONFLICT (project_id) DO NOTHING
            """, projectId);
    }

    private Integer updateReturning(UUID projectId) {
        List<Integer> rows = jdbcTemplate.query(
            """
                UPDATE project_issue_counter
                SET next_number = next_number + 1
                WHERE project_id = ?
                RETURNING next_number
                """,
            (rs, rowNum) -> rs.getInt(1),
            projectId);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
