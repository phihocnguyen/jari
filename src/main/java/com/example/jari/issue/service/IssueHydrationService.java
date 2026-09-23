package com.example.jari.issue.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.sprint.entity.SprintIssue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Loads issue collections in batched queries to avoid N+1 when mapping {@link com.example.jari.issue.dto.IssueResponse}.
 * Hibernate cannot fetch multiple bag collections in one query — use separate SELECTs per association.
 */
@Service
public class IssueHydrationService {

    @PersistenceContext
    private EntityManager em;

    public void hydrateCollections(Collection<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        List<UUID> ids = issues.stream().map(Issue::getId).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }

        em.createQuery("""
                SELECT DISTINCT i FROM Issue i
                LEFT JOIN FETCH i.labels
                WHERE i.id IN :ids
                """, Issue.class)
            .setParameter("ids", ids)
            .getResultList();

        em.createQuery("""
                SELECT DISTINCT i FROM Issue i
                LEFT JOIN FETCH i.components c
                LEFT JOIN FETCH c.project
                WHERE i.id IN :ids
                """, Issue.class)
            .setParameter("ids", ids)
            .getResultList();

        em.createQuery("""
                SELECT DISTINCT si FROM SprintIssue si
                JOIN FETCH si.sprint
                WHERE si.issue.id IN :ids
                """, SprintIssue.class)
            .setParameter("ids", ids)
            .getResultList();
    }

    public Issue hydrateCollections(Issue issue) {
        if (issue != null) {
            hydrateCollections(List.of(issue));
        }
        return issue;
    }
}
