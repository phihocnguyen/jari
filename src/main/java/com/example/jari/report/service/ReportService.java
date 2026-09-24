package com.example.jari.report.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueHistory;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.report.dto.ProjectReportsResponse;
import com.example.jari.report.dto.ProjectReportsResponse.*;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintStatus;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int DEFAULT_FLOW_DAYS = 14;
    private static final int DEFAULT_VELOCITY_SPRINTS = 8;

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public ProjectReportsResponse getReports(UUID projectId, UUID sprintId, Integer days, Integer velocityLimit) {
        projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        int windowDays = days != null && days > 0 ? Math.min(days, 90) : DEFAULT_FLOW_DAYS;
        int sprintLimit = velocityLimit != null && velocityLimit > 0
            ? Math.min(velocityLimit, 20) : DEFAULT_VELOCITY_SPRINTS;

        return ProjectReportsResponse.builder()
            .burndown(buildBurndown(projectId, sprintId))
            .velocity(buildVelocity(projectId, sprintLimit))
            .cumulativeFlow(buildCumulativeFlow(projectId, windowDays))
            .createdVsResolved(buildCreatedVsResolved(projectId, windowDays))
            .build();
    }

    // ── Burndown ────────────────────────────────────────────────────

    private BurndownReport buildBurndown(UUID projectId, UUID sprintId) {
        Sprint sprint = resolveSprintForBurndown(projectId, sprintId);
        if (sprint == null) {
            return BurndownReport.builder()
                .message("No active or completed sprint with dates found for burndown.")
                .points(List.of())
                .totalStoryPoints(BigDecimal.ZERO)
                .build();
        }

        LocalDate start = toLocalDate(sprint.getStartDate());
        LocalDate end = toLocalDate(sprint.getEndDate());
        if (start == null || end == null || end.isBefore(start)) {
            return BurndownReport.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .sprintStatus(sprint.getStatus().name())
                .message("Sprint needs startDate and endDate for burndown.")
                .points(List.of())
                .totalStoryPoints(BigDecimal.ZERO)
                .build();
        }

        List<SprintIssue> sprintIssues = sprintIssueRepository.findBySprintIdWithIssues(sprint.getId());
        Map<UUID, LocalDate> doneOn = resolveDoneDates(sprintIssues);

        BigDecimal totalSp = sprintIssues.stream()
            .map(si -> sp(si.getIssue()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        LocalDate last = end.isAfter(today) ? today : end;
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, end));

        List<BurndownPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(last); day = day.plusDays(1)) {
            long elapsed = ChronoUnit.DAYS.between(start, day);
            BigDecimal ideal = totalSp.multiply(BigDecimal.valueOf(totalDays - elapsed))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            if (ideal.compareTo(BigDecimal.ZERO) < 0) {
                ideal = BigDecimal.ZERO;
            }

            BigDecimal remaining = BigDecimal.ZERO;
            for (SprintIssue si : sprintIssues) {
                Issue issue = si.getIssue();
                LocalDate completed = doneOn.get(issue.getId());
                if (completed == null || completed.isAfter(day)) {
                    remaining = remaining.add(sp(issue));
                }
            }

            points.add(BurndownPoint.builder()
                .date(day)
                .idealRemaining(ideal)
                .actualRemaining(remaining.setScale(2, RoundingMode.HALF_UP))
                .build());
        }

        return BurndownReport.builder()
            .sprintId(sprint.getId())
            .sprintName(sprint.getName())
            .sprintStatus(sprint.getStatus().name())
            .startDate(start)
            .endDate(end)
            .totalStoryPoints(totalSp.setScale(2, RoundingMode.HALF_UP))
            .points(points)
            .build();
    }

    private Sprint resolveSprintForBurndown(UUID projectId, UUID sprintId) {
        if (sprintId != null) {
            return sprintRepository.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));
        }
        Optional<Sprint> active = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        if (active.isPresent()) {
            return active.get();
        }
        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
            .filter(s -> s.getStatus() == SprintStatus.COMPLETED)
            .findFirst()
            .orElse(null);
    }

    private Map<UUID, LocalDate> resolveDoneDates(List<SprintIssue> sprintIssues) {
        List<UUID> issueIds = sprintIssues.stream().map(si -> si.getIssue().getId()).toList();
        Map<UUID, LocalDate> doneOn = new HashMap<>();
        if (issueIds.isEmpty()) {
            return doneOn;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT h.issue.id, MAX(h.createdAt)
            FROM IssueHistory h
            WHERE h.issue.id IN :ids
              AND LOWER(h.field) = 'status'
              AND (
                LOWER(h.newValue) = 'done'
                OR LOWER(h.newValue) LIKE '%done%'
              )
            GROUP BY h.issue.id
            """)
            .setParameter("ids", issueIds)
            .getResultList();

        for (Object[] row : rows) {
            doneOn.put((UUID) row[0], ((OffsetDateTime) row[1]).toLocalDate());
        }

        // Fallback: currently DONE issues without history use updatedAt / today
        for (SprintIssue si : sprintIssues) {
            Issue issue = si.getIssue();
            if (doneOn.containsKey(issue.getId())) {
                continue;
            }
            if (isDone(issue)) {
                LocalDate when = issue.getUpdatedAt() != null
                    ? issue.getUpdatedAt().toLocalDate()
                    : LocalDate.now();
                doneOn.put(issue.getId(), when);
            }
        }
        return doneOn;
    }

    // ── Velocity ────────────────────────────────────────────────────

    private VelocityReport buildVelocity(UUID projectId, int limit) {
        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
            .filter(s -> s.getStatus() == SprintStatus.COMPLETED || s.getStatus() == SprintStatus.ACTIVE)
            .limit(limit)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(sprints); // chronological for chart

        List<VelocitySprint> rows = new ArrayList<>();
        BigDecimal completedSum = BigDecimal.ZERO;
        int completedCount = 0;

        for (Sprint sprint : sprints) {
            List<SprintIssue> issues = sprintIssueRepository.findBySprintIdWithIssues(sprint.getId());
            BigDecimal committed = issues.stream().map(si -> sp(si.getIssue())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal completed = issues.stream()
                .filter(si -> isDone(si.getIssue()))
                .map(si -> sp(si.getIssue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(VelocitySprint.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .sprintStatus(sprint.getStatus().name())
                .committed(committed.setScale(2, RoundingMode.HALF_UP))
                .completed(completed.setScale(2, RoundingMode.HALF_UP))
                .build());

            if (sprint.getStatus() == SprintStatus.COMPLETED) {
                completedSum = completedSum.add(completed);
                completedCount++;
            }
        }

        BigDecimal avg = completedCount == 0
            ? BigDecimal.ZERO
            : completedSum.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);

        return VelocityReport.builder()
            .averageCompleted(avg)
            .sprints(rows)
            .build();
    }

    // ── Cumulative Flow ─────────────────────────────────────────────

    private CumulativeFlowReport buildCumulativeFlow(UUID projectId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        List<Issue> issues = em.createQuery("""
            SELECT i FROM Issue i
            JOIN FETCH i.status
            WHERE i.project.id = :pid
            """, Issue.class)
            .setParameter("pid", projectId)
            .getResultList();

        if (issues.isEmpty()) {
            return CumulativeFlowReport.builder().points(emptyFlowPoints(start, end)).build();
        }

        List<UUID> ids = issues.stream().map(Issue::getId).toList();
        List<IssueHistory> histories = em.createQuery("""
            SELECT h FROM IssueHistory h
            JOIN FETCH h.issue
            WHERE h.issue.id IN :ids AND LOWER(h.field) = 'status'
            ORDER BY h.createdAt ASC
            """, IssueHistory.class)
            .setParameter("ids", ids)
            .getResultList();

        Map<UUID, List<IssueHistory>> byIssue = histories.stream()
            .collect(Collectors.groupingBy(h -> h.getIssue().getId()));

        // Earliest known category before window: walk history
        Map<UUID, String> categoryAt = new HashMap<>();
        for (Issue issue : issues) {
            categoryAt.put(issue.getId(), normalizeCategory(
                issue.getStatus() != null ? issue.getStatus().getCategory() : "TODO",
                issue.getStatus() != null ? issue.getStatus().getName() : null));
        }

        // Replay history backwards conceptually: for each day, apply events up to end of day
        // Start from beginning: find initial category as first history oldValue or current
        Map<UUID, String> state = new HashMap<>();
        for (Issue issue : issues) {
            List<IssueHistory> events = byIssue.getOrDefault(issue.getId(), List.of());
            if (events.isEmpty()) {
                state.put(issue.getId(), categoryAt.get(issue.getId()));
            } else {
                IssueHistory first = events.get(0);
                state.put(issue.getId(), normalizeCategory(null, first.getOldValue()));
            }
        }

        Map<UUID, Integer> nextEventIdx = new HashMap<>();
        byIssue.keySet().forEach(id -> nextEventIdx.put(id, 0));

        List<CumulativeFlowPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            OffsetDateTime dayEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

            for (Issue issue : issues) {
                List<IssueHistory> events = byIssue.getOrDefault(issue.getId(), List.of());
                int idx = nextEventIdx.getOrDefault(issue.getId(), 0);
                while (idx < events.size() && !events.get(idx).getCreatedAt().isAfter(dayEnd.minusNanos(1))) {
                    state.put(issue.getId(), normalizeCategory(null, events.get(idx).getNewValue()));
                    idx++;
                }
                nextEventIdx.put(issue.getId(), idx);
            }

            long todo = 0, inProgress = 0, done = 0;
            for (String cat : state.values()) {
                switch (cat) {
                    case "DONE" -> done++;
                    case "IN_PROGRESS" -> inProgress++;
                    default -> todo++;
                }
            }
            points.add(CumulativeFlowPoint.builder()
                .date(day)
                .todo(todo)
                .inProgress(inProgress)
                .done(done)
                .build());
        }

        return CumulativeFlowReport.builder().points(points).build();
    }

    private List<CumulativeFlowPoint> emptyFlowPoints(LocalDate start, LocalDate end) {
        List<CumulativeFlowPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            points.add(CumulativeFlowPoint.builder().date(day).todo(0).inProgress(0).done(0).build());
        }
        return points;
    }

    // ── Created vs Resolved ─────────────────────────────────────────

    private CreatedVsResolvedReport buildCreatedVsResolved(UUID projectId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        OffsetDateTime from = start.atStartOfDay().atOffset(ZoneOffset.UTC);

        @SuppressWarnings("unchecked")
        List<OffsetDateTime> createdAts = em.createQuery("""
            SELECT i.createdAt FROM Issue i
            WHERE i.project.id = :pid AND i.createdAt >= :from
            """)
            .setParameter("pid", projectId)
            .setParameter("from", from)
            .getResultList();

        @SuppressWarnings("unchecked")
        List<OffsetDateTime> resolvedAts = em.createQuery("""
            SELECT h.createdAt FROM IssueHistory h
            JOIN h.issue i
            WHERE i.project.id = :pid
              AND h.createdAt >= :from
              AND LOWER(h.field) = 'status'
              AND (
                LOWER(h.newValue) = 'done'
                OR LOWER(COALESCE(h.newValue, '')) LIKE '%done%'
              )
            """)
            .setParameter("pid", projectId)
            .setParameter("from", from)
            .getResultList();

        Map<LocalDate, Long> createdByDay = new HashMap<>();
        for (OffsetDateTime ts : createdAts) {
            if (ts != null) {
                createdByDay.merge(ts.toLocalDate(), 1L, Long::sum);
            }
        }
        Map<LocalDate, Long> resolvedByDay = new HashMap<>();
        for (OffsetDateTime ts : resolvedAts) {
            if (ts != null) {
                resolvedByDay.merge(ts.toLocalDate(), 1L, Long::sum);
            }
        }

        List<CreatedVsResolvedPoint> points = new ArrayList<>();
        long totalCreated = 0;
        long totalResolved = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            long c = createdByDay.getOrDefault(day, 0L);
            long r = resolvedByDay.getOrDefault(day, 0L);
            totalCreated += c;
            totalResolved += r;
            points.add(CreatedVsResolvedPoint.builder()
                .date(day)
                .created(c)
                .resolved(r)
                .build());
        }

        double rate = totalCreated == 0 ? 0.0
            : Math.min(1.0, (double) totalResolved / (double) totalCreated);

        return CreatedVsResolvedReport.builder()
            .resolutionRate(rate)
            .totalCreated(totalCreated)
            .totalResolved(totalResolved)
            .points(points)
            .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static BigDecimal sp(Issue issue) {
        return issue.getStoryPoints() != null ? issue.getStoryPoints() : BigDecimal.ZERO;
    }

    private static boolean isDone(Issue issue) {
        if (issue.getStatus() == null) {
            return false;
        }
        String cat = issue.getStatus().getCategory();
        String name = issue.getStatus().getName();
        return "DONE".equalsIgnoreCase(cat) || (name != null && name.equalsIgnoreCase("DONE"));
    }

    private static String normalizeCategory(String category, String statusName) {
        if (category != null) {
            String c = category.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (c.contains("DONE") || c.contains("COMPLETE")) {
                return "DONE";
            }
            if (c.contains("PROGRESS") || c.contains("REVIEW") || c.contains("TEST")) {
                return "IN_PROGRESS";
            }
            if (c.contains("TODO") || c.contains("TO_DO") || c.contains("BACKLOG") || c.contains("OPEN")) {
                return "TODO";
            }
        }
        if (statusName != null) {
            String n = statusName.trim().toUpperCase(Locale.ROOT);
            if (n.contains("DONE") || n.contains("COMPLETE") || n.contains("CLOSED") || n.contains("RESOLVED")) {
                return "DONE";
            }
            if (n.contains("PROGRESS") || n.contains("REVIEW") || n.contains("TEST") || n.contains("DEV")) {
                return "IN_PROGRESS";
            }
        }
        return "TODO";
    }

    private static LocalDate toLocalDate(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDate();
    }
}
