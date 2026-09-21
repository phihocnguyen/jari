package com.example.jari.automation.service;

import com.example.jari.automation.dto.AutomationLogResponse;
import com.example.jari.automation.dto.RunAutomationRequest;
import com.example.jari.automation.entity.IssueAutomationLog;
import com.example.jari.automation.repository.IssueAutomationLogRepository;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.Status;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final IssueAutomationLogRepository logRepository;
    private final IssueRepository issueRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final StatusRepository statusRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AutomationLogResponse> listLogs(UUID issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }
        return logRepository.findByIssueIdOrderByExecutedAtDesc(issueId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void recordLog(Issue issue, String ruleName, String status, String description) {
        try {
            IssueAutomationLog autoLog = IssueAutomationLog.builder()
                .issue(issue)
                .ruleName(ruleName)
                .status(status)
                .description(description)
                .build();
            logRepository.save(autoLog);
        } catch (Exception e) {
            log.warn("Failed to record automation log: {}", e.getMessage());
        }
    }

    /**
     * Executes automatic rules on status transitions.
     */
    @Transactional
    public void onIssueStatusChanged(Issue issue, User actor) {
        if (issue == null || issue.getStatus() == null) return;

        String currentStatusName = issue.getStatus().getName();
        String currentCategory = issue.getStatus().getCategory();

        // Rule 1: Auto-assign unassigned issue to actor when transitioned to IN PROGRESS
        boolean isInProgress = "IN_PROGRESS".equalsIgnoreCase(currentCategory) ||
                               "IN PROGRESS".equalsIgnoreCase(currentStatusName);
        if (isInProgress && issue.getAssignee() == null && actor != null) {
            issue.setAssignee(actor);
            issueRepository.save(issue);
            eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(issue.getId()));
            recordLog(issue, "Auto-assign on In Progress", "SUCCESS",
                "Automatically assigned unassigned issue to " + actor.getDisplayName() + " upon moving to In Progress.");
        }

        // Rule 2: If issue is a subtask (has parent) and moved to DONE, check if all sibling subtasks are DONE
        boolean isDone = "DONE".equalsIgnoreCase(currentCategory) ||
                         "DONE".equalsIgnoreCase(currentStatusName);
        if (isDone && issue.getParent() != null) {
            Issue parent = issue.getParent();
            List<Issue> siblings = issueRepository.findByParentId(parent.getId());
            boolean allDone = siblings.stream().allMatch(sib ->
                sib.getId().equals(issue.getId()) ||
                (sib.getStatus() != null &&
                 ("DONE".equalsIgnoreCase(sib.getStatus().getCategory()) ||
                  "DONE".equalsIgnoreCase(sib.getStatus().getName())))
            );

            if (allDone && (parent.getStatus() == null || !"DONE".equalsIgnoreCase(parent.getStatus().getCategory()))) {
                Optional<Status> doneStatus = statusRepository.findByCategoryIgnoreCase("DONE")
                    .or(() -> statusRepository.findByName("DONE"));
                if (doneStatus.isPresent()) {
                    parent.setStatus(doneStatus.get());
                    issueRepository.save(parent);
                    eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(parent.getId()));
                    recordLog(parent, "Auto-close parent when subtasks complete", "SUCCESS",
                        "All subtasks are complete. Automatically transitioned parent issue " + parent.getIssueKey() + " to DONE.");
                }
            }
        }
    }

    @Transactional
    public AutomationLogResponse runManualRule(UUID issueId, UUID actorId, RunAutomationRequest req) {
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        String rule = req.getRule() != null ? req.getRule().trim().toUpperCase() : "";
        String desc;
        String status = "SUCCESS";

        switch (rule) {
            case "AUTO_CLOSE_PARENT" -> {
                List<Issue> subtasks = issueRepository.findByParentId(issue.getId());
                if (subtasks.isEmpty()) {
                    desc = "No subtasks found for this issue.";
                    status = "TRIGGERED";
                } else {
                    boolean allDone = subtasks.stream().allMatch(s -> s.getStatus() != null &&
                        ("DONE".equalsIgnoreCase(s.getStatus().getCategory()) || "DONE".equalsIgnoreCase(s.getStatus().getName())));
                    if (allDone) {
                        Optional<Status> doneStatus = statusRepository.findByCategoryIgnoreCase("DONE")
                            .or(() -> statusRepository.findByName("DONE"));
                        doneStatus.ifPresent(issue::setStatus);
                        issueRepository.save(issue);
                        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(issue.getId()));
                        desc = "Evaluated all subtasks: all " + subtasks.size() + " subtasks are DONE. Updated parent to DONE.";
                    } else {
                        desc = "Evaluated subtasks: not all subtasks are DONE yet.";
                        status = "TRIGGERED";
                    }
                }
            }
            case "AUTO_ASSIGN_ME" -> {
                if (actor != null) {
                    issue.setAssignee(actor);
                    issueRepository.save(issue);
                    desc = "Assigned issue to " + actor.getDisplayName() + ".";
                } else {
                    desc = "Actor not found for self-assignment.";
                    status = "FAILED";
                }
            }
            default -> {
                desc = "Executed custom automation verification rule on issue " + issue.getIssueKey() + ".";
            }
        }

        IssueAutomationLog autoLog = IssueAutomationLog.builder()
            .issue(issue)
            .ruleName(req.getRule())
            .status(status)
            .description(desc)
            .build();

        return toResponse(logRepository.save(autoLog));
    }

    private AutomationLogResponse toResponse(IssueAutomationLog autoLog) {
        return AutomationLogResponse.builder()
            .id(autoLog.getId())
            .issueId(autoLog.getIssue() != null ? autoLog.getIssue().getId() : null)
            .ruleName(autoLog.getRuleName())
            .status(autoLog.getStatus())
            .description(autoLog.getDescription())
            .executedAt(autoLog.getExecutedAt())
            .build();
    }
}
