package com.example.jari.notification.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.notification.dto.NotificationCreatedEvent;
import com.example.jari.notification.dto.NotificationResponse;
import com.example.jari.notification.entity.Notification;
import com.example.jari.notification.entity.NotificationType;
import com.example.jari.notification.repository.NotificationRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.workspace.entity.Workspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Window (in days) for the "deadline approaching" reminder: due date <= today + 2. */
    public static final int DUE_SOON_WINDOW_DAYS = 2;

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Domain triggers ──────────────────────────────────────────────

    /**
     * Notify a user that an issue was assigned to them. Self-assignment does not notify.
     */
    @Transactional
    public void notifyIssueAssigned(Issue issue, User actor, User assignee) {
        if (assignee == null) return;
        String message = String.format("%s assigned you to %s: %s",
            displayName(actor), issue.getIssueKey(), issue.getTitle());
        create(NotificationType.ISSUE_ASSIGNED, assignee, issue.getProject(), issue, null, message);
    }

    @Transactional
    public void notifyIssueUpdated(Issue issue, User actor, User recipient) {
        if (recipient == null || isSelf(actor, recipient)) return;
        String message = String.format("%s updated issue %s: %s",
            displayName(actor), issue.getIssueKey(), issue.getTitle());
        create(NotificationType.ISSUE_UPDATED, recipient, issue.getProject(), issue, null, message);
    }

    @Transactional
    public void notifyIssueCommented(Issue issue, User actor, User recipient) {
        if (recipient == null || isSelf(actor, recipient)) return;
        String message = String.format("%s commented on issue %s: %s",
            displayName(actor), issue.getIssueKey(), issue.getTitle());
        create(NotificationType.ISSUE_COMMENTED, recipient, issue.getProject(), issue, null, message);
    }

    /**
     * Notify a user that they were added to a project.
     */
    @Transactional
    public void notifyProjectMemberAdded(Project project, User actor, User newMember) {
        if (newMember == null) return;
        String message = String.format("%s added you to project %s",
            displayName(actor), project.getName());
        create(NotificationType.MEMBER_INVITED, newMember, project, null, null, message);
    }

    /**
     * Notify a user that they were added to a workspace.
     */
    @Transactional
    public void notifyWorkspaceMemberAdded(Workspace workspace, User actor, User newMember) {
        if (newMember == null) return;
        String message = String.format("%s added you to workspace %s",
            displayName(actor), workspace.getName());
        create(NotificationType.MEMBER_INVITED, newMember, null, null, workspace, message);
    }

    /**
     * Notify the assignee that an issue's deadline is near (due date <= today + 2, including overdue).
     * De-duplicated to at most one reminder per issue, recipient and day.
     */
    @Transactional
    public void notifyIssueDueSoon(Issue issue, User assignee) {
        if (assignee == null || issue.getDueDate() == null) return;
        if (issue.getDueDate().isAfter(LocalDate.now().plusDays(DUE_SOON_WINDOW_DAYS))) return;

        // Midnight of today (absolute instant); only the instant matters for the timestamptz comparison
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().atOffset(ZoneOffset.UTC);
        boolean alreadyNotifiedToday = notificationRepository
            .existsByTypeAndIssueIdAndRecipientIdAndCreatedAtGreaterThanEqual(
                NotificationType.ISSUE_DUE_SOON, issue.getId(), assignee.getId(), startOfDay);
        if (alreadyNotifiedToday) return;

        String message = String.format("%s '%s' %s", issue.getIssueKey(), issue.getTitle(), duePhrase(issue.getDueDate()));
        create(NotificationType.ISSUE_DUE_SOON, assignee, issue.getProject(), issue, null, message);
    }

    // ─── Inbox API ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(UUID userId) {
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findByIdAndRecipientId(notificationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (n.getReadAt() == null) n.setReadAt(OffsetDateTime.now());
        return toResponse(n);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByRecipientId(userId, OffsetDateTime.now());
    }

    // ─── Internals ────────────────────────────────────────────────────

    /**
     * Persists the notification inside the caller's transaction and publishes an event;
     * RabbitMQEventBridge forwards it to RabbitMQ after commit, and NotificationConsumer
     * pushes over STOMP — a rollback never sends a phantom notification.
     */
    private void create(NotificationType type, User recipient, Project project, Issue issue,
                        Workspace workspace, String message) {
        try {
            Notification n = notificationRepository.saveAndFlush(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .issue(issue)
                .issueKey(issue != null ? issue.getIssueKey() : null)
                .project(project)
                .projectName(project != null ? project.getName() : null)
                .workspace(workspace)
                .workspaceName(workspace != null ? workspace.getName() : null)
                .build());
            eventPublisher.publishEvent(new NotificationCreatedEvent(toResponse(n)));
        } catch (Exception e) {
            // A notification failure must never break the business operation that triggered it.
            log.error("Failed to create notification (type={}, recipient={}): {}",
                type, recipient != null ? recipient.getId() : null, e.getMessage(), e);
        }
    }

    private String duePhrase(LocalDate dueDate) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (daysLeft < 0) return "is overdue";
        if (daysLeft == 0) return "is due today";
        if (daysLeft == 1) return "is due tomorrow";
        return String.format("is due in %d days", daysLeft);
    }

    private boolean isSelf(User actor, User recipient) {
        return actor != null && recipient != null && actor.getId().equals(recipient.getId());
    }

    private String displayName(User u) {
        return u != null ? u.getDisplayName() : "Someone";
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .type(n.getType() != null ? n.getType().name() : null)
            .targetUserId(n.getRecipient() != null ? n.getRecipient().getId() : null)
            .issueId(n.getIssue() != null ? n.getIssue().getId() : null)
            .issueKey(n.getIssueKey())
            .projectId(n.getProject() != null ? n.getProject().getId() : null)
            .projectName(n.getProjectName())
            .workspaceId(n.getWorkspace() != null ? n.getWorkspace().getId() : null)
            .workspaceName(n.getWorkspaceName())
            .message(n.getMessage())
            .read(n.getReadAt() != null)
            .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toInstant() : Instant.now())
            .build();
    }
}
