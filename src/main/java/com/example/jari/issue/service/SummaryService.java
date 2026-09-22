package com.example.jari.issue.service;

import com.example.jari.issue.dto.ProjectSummaryResponse;
import com.example.jari.issue.dto.ProjectSummaryResponse.*;
import com.example.jari.issue.entity.IssueHistory;
import com.example.jari.issue.repository.IssueHistoryRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.CacheNames;
import com.example.jari.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final IssueRepository issueRepository;
    private final IssueHistoryRepository historyRepository;
    private final ProjectRepository projectRepository;

    @PersistenceContext
    private EntityManager em;

    @Cacheable(value = CacheNames.PROJECT_SUMMARY, key = "#projectId")
    @Transactional(readOnly = true)
    public ProjectSummaryResponse getSummary(UUID projectId) {
        projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        return ProjectSummaryResponse.builder()
            .metrics(buildMetrics(projectId))
            .statusBreakdown(buildStatusBreakdown(projectId))
            .priorityBreakdown(buildPriorityBreakdown(projectId))
            .typeBreakdown(buildTypeBreakdown(projectId))
            .teamWorkload(buildTeamWorkload(projectId))
            .recentActivity(buildRecentActivity(projectId))
            .epicProgress(buildEpicProgress(projectId))
            .build();
    }

    // ── 1. Metrics ──────────────────────────────────────────────────
    private Metrics buildMetrics(UUID projectId) {
        OffsetDateTime sevenDaysAgo  = OffsetDateTime.now().minusDays(7);
        OffsetDateTime sevenDaysFrom = OffsetDateTime.now().plusDays(7);
        LocalDate today              = LocalDate.now();
        LocalDate todayPlus7         = today.plusDays(7);

        long completed = ((Number) em.createQuery(
            "SELECT COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.status.category = 'DONE' " +
            "AND i.updatedAt >= :since")
            .setParameter("pid", projectId)
            .setParameter("since", sevenDaysAgo)
            .getSingleResult()).longValue();

        long updated = ((Number) em.createQuery(
            "SELECT COUNT(DISTINCT i.id) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.updatedAt >= :since")
            .setParameter("pid", projectId)
            .setParameter("since", sevenDaysAgo)
            .getSingleResult()).longValue();

        long created = ((Number) em.createQuery(
            "SELECT COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.createdAt >= :since")
            .setParameter("pid", projectId)
            .setParameter("since", sevenDaysAgo)
            .getSingleResult()).longValue();

        long due = ((Number) em.createQuery(
            "SELECT COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.dueDate >= :today AND i.dueDate <= :plus7 " +
            "AND i.status.category <> 'DONE'")
            .setParameter("pid", projectId)
            .setParameter("today", today)
            .setParameter("plus7", todayPlus7)
            .getSingleResult()).longValue();

        return Metrics.builder()
            .completedLast7Days(completed)
            .updatedLast7Days(updated)
            .createdLast7Days(created)
            .dueNext7Days(due)
            .build();
    }

    // ── 2. Status Breakdown ─────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<StatusCount> buildStatusBreakdown(UUID projectId) {
        List<Object[]> rows = em.createQuery(
            "SELECT i.status.name, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid GROUP BY i.status.name ORDER BY COUNT(i) DESC")
            .setParameter("pid", projectId)
            .getResultList();
        return rows.stream()
            .map(r -> StatusCount.builder()
                .status((String) r[0])
                .count(((Number) r[1]).longValue())
                .build())
            .collect(Collectors.toList());
    }

    // ── 3. Priority Breakdown ───────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<PriorityCount> buildPriorityBreakdown(UUID projectId) {
        List<Object[]> rows = em.createQuery(
            "SELECT i.priority.name, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid GROUP BY i.priority.name ORDER BY COUNT(i) DESC")
            .setParameter("pid", projectId)
            .getResultList();
        return rows.stream()
            .map(r -> PriorityCount.builder()
                .priority((String) r[0])
                .count(((Number) r[1]).longValue())
                .build())
            .collect(Collectors.toList());
    }

    // ── 4. Type Breakdown ───────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<TypeCount> buildTypeBreakdown(UUID projectId) {
        List<Object[]> rows = em.createQuery(
            "SELECT i.issueType.name, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid GROUP BY i.issueType.name ORDER BY COUNT(i) DESC")
            .setParameter("pid", projectId)
            .getResultList();
        return rows.stream()
            .map(r -> TypeCount.builder()
                .type((String) r[0])
                .count(((Number) r[1]).longValue())
                .build())
            .collect(Collectors.toList());
    }

    // ── 5. Team Workload ────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<MemberWorkload> buildTeamWorkload(UUID projectId) {
        List<Object[]> totalRows = em.createQuery(
            "SELECT i.assignee.id, i.assignee.displayName, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.assignee IS NOT NULL " +
            "GROUP BY i.assignee.id, i.assignee.displayName ORDER BY COUNT(i) DESC")
            .setParameter("pid", projectId)
            .getResultList();

        if (totalRows.isEmpty()) return Collections.emptyList();

        List<Object[]> inProgressRows = em.createQuery(
            "SELECT i.assignee.id, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.assignee IS NOT NULL " +
            "AND i.status.category = 'IN_PROGRESS' " +
            "GROUP BY i.assignee.id")
            .setParameter("pid", projectId)
            .getResultList();
        Map<UUID, Long> ipMap = new HashMap<>();
        for (Object[] r : inProgressRows) {
            ipMap.put((UUID) r[0], ((Number) r[1]).longValue());
        }

        long maxCount = ((Number) totalRows.get(0)[2]).longValue();
        if (maxCount == 0) maxCount = 1;

        List<MemberWorkload> result = new ArrayList<>();
        for (Object[] r : totalRows) {
            UUID uid = (UUID) r[0];
            long assigned = ((Number) r[2]).longValue();
            int percent = (int) Math.round((double) assigned / maxCount * 100);
            result.add(MemberWorkload.builder()
                .userId(uid)
                .fullName((String) r[1])
                .assignedCount(assigned)
                .inProgressCount(ipMap.getOrDefault(uid, 0L))
                .percent(percent)
                .build());
        }
        return result;
    }

    // ── 6. Recent Activity ──────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<ActivityItem> buildRecentActivity(UUID projectId) {
        List<IssueHistory> histories = em.createQuery(
            "SELECT h FROM IssueHistory h " +
            "JOIN FETCH h.issue i JOIN FETCH h.user u " +
            "WHERE i.project.id = :pid " +
            "ORDER BY h.createdAt DESC", IssueHistory.class)
            .setParameter("pid", projectId)
            .setMaxResults(10)
            .getResultList();

        return histories.stream()
            .map(h -> {
                String action = buildActionLabel(h.getField(), h.getNewValue());
                return ActivityItem.builder()
                    .actorName(h.getUser().getDisplayName())
                    .action(action)
                    .issueKey(h.getIssue().getIssueKey())
                    .issueTitle(h.getIssue().getTitle())
                    .occurredAt(h.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());
    }

    private String buildActionLabel(String field, String newValue) {
        if (field == null) return "updated";
        return switch (field.toLowerCase()) {
            case "status"      -> "changed status to " + newValue;
            case "assignee"    -> "reassigned to " + newValue;
            case "priority"    -> "changed priority to " + newValue;
            case "title"       -> "renamed issue";
            case "description" -> "updated the description";
            case "sprint"      -> "moved to sprint " + newValue;
            default            -> "updated " + field;
        };
    }

    // ── 7. Epic Progress ────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<EpicProgress> buildEpicProgress(UUID projectId) {
        // Fetch all epics for the project
        List<Object[]> epics = em.createQuery(
            "SELECT i.id, i.issueKey, i.title FROM Issue i " +
            "WHERE i.project.id = :pid AND UPPER(i.issueType.name) = 'EPIC' " +
            "ORDER BY i.createdAt ASC")
            .setParameter("pid", projectId)
            .getResultList();

        if (epics.isEmpty()) return Collections.emptyList();

        // Fetch all child issues grouped by parentId and status category
        List<Object[]> children = em.createQuery(
            "SELECT i.parent.id, i.status.category, COUNT(i) FROM Issue i " +
            "WHERE i.project.id = :pid AND i.parent IS NOT NULL " +
            "GROUP BY i.parent.id, i.status.category")
            .setParameter("pid", projectId)
            .getResultList();

        // Build map: epicId -> { category -> count }
        Map<UUID, Map<String, Long>> epicChildMap = new HashMap<>();
        for (Object[] row : children) {
            UUID parentId = (UUID) row[0];
            String category = (String) row[1];
            long count = ((Number) row[2]).longValue();
            epicChildMap.computeIfAbsent(parentId, k -> new HashMap<>()).put(category, count);
        }

        List<EpicProgress> result = new ArrayList<>();
        for (Object[] epic : epics) {
            UUID epicId    = (UUID) epic[0];
            String epicKey = (String) epic[1];
            String title   = (String) epic[2];

            Map<String, Long> cats = epicChildMap.getOrDefault(epicId, Collections.emptyMap());
            long done       = cats.getOrDefault("DONE", 0L);
            long inProgress = cats.getOrDefault("IN_PROGRESS", 0L);
            long todo       = cats.getOrDefault("TODO", 0L);
            long total      = done + inProgress + todo;

            int doneP = total == 0 ? 0 : (int) Math.round((double) done / total * 100);
            int ipP   = total == 0 ? 0 : (int) Math.round((double) inProgress / total * 100);
            int todoP = total == 0 ? 0 : Math.max(0, 100 - doneP - ipP);

            result.add(EpicProgress.builder()
                .epicId(epicId)
                .epicKey(epicKey)
                .epicTitle(title)
                .total((int) total)
                .doneCount((int) done)
                .inProgressCount((int) inProgress)
                .todoCount((int) todo)
                .donePercent(doneP)
                .inProgressPercent(ipP)
                .todoPercent(todoP)
                .build());
        }
        return result;
    }
}
