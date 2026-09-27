package com.example.jari.issue.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.sprint.entity.SprintIssue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.collection.spi.PersistentCollection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

        // Avoid NonUniqueObjectException: do not JOIN FETCH SprintIssue when already in the PC
        // (e.g. cascaded on create). Still fetch for lazy PersistentCollections.
        List<UUID> needsSprintFetch = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.getId() != null && !sprintIssuesAlreadyPresent(issue)) {
                needsSprintFetch.add(issue.getId());
            }
        }
        if (!needsSprintFetch.isEmpty()) {
            em.createQuery("""
                    SELECT DISTINCT si FROM SprintIssue si
                    JOIN FETCH si.sprint
                    WHERE si.issue.id IN :ids
                    """, SprintIssue.class)
                .setParameter("ids", needsSprintFetch)
                .getResultList();
        }
    }

    public Issue hydrateCollections(Issue issue) {
        if (issue != null) {
            hydrateCollections(List.of(issue));
        }
        return issue;
    }

    private static boolean sprintIssuesAlreadyPresent(Issue issue) {
        Set<SprintIssue> col = issue.getSprintIssues();
        if (col == null) {
            return false;
        }
        if (col instanceof PersistentCollection<?> persistent) {
            return persistent.wasInitialized();
        }
        // Transient HashSet already populated in this TX (create + cascade)
        return !col.isEmpty();
    }
}
