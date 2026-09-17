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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Domain triggers ──────────────────────────────────────────────

    /**
     * Notify a user that an issue was assigned to them. Self-assignment does not notify.
     */
    @Transactional
    public void notifyIssueAssigned(Issue issue, User actor, User assignee) {
        if (assignee == null || isSelf(actor, assignee)) return;
        String message = String.format("%s assigned you to %s: %s",
            displayName(actor), issue.getIssueKey(), issue.getTitle());
        create(NotificationType.ISSUE_ASSIGNED, assignee, issue.getProject(), issue, message);
    }

    /**
     * Notify a user that they were added to a project. Self-add (project creator) does not notify.
     */
    @Transactional
    public void notifyProjectMemberAdded(Project project, User actor, User newMember) {
        if (newMember == null || isSelf(actor, newMember)) return;
        String message = String.format("%s added you to project %s",
            displayName(actor), project.getName());
        create(NotificationType.MEMBER_INVITED, newMember, project, null, message);
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
     * the WebSocket push is done by NotificationPushListener after the transaction commits,
     * so a rollback never sends a phantom notification.
     */
    private void create(NotificationType type, User recipient, Project project, Issue issue, String message) {
        try {
            Notification n = notificationRepository.saveAndFlush(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .issue(issue)
                .issueKey(issue != null ? issue.getIssueKey() : null)
                .project(project)
                .projectName(project != null ? project.getName() : null)
                .build());
            eventPublisher.publishEvent(new NotificationCreatedEvent(toResponse(n)));
        } catch (Exception e) {
            // A notification failure must never break the business operation that triggered it.
            log.error("Failed to create notification (type={}, recipient={}): {}",
                type, recipient != null ? recipient.getId() : null, e.getMessage(), e);
        }
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
            .message(n.getMessage())
            .read(n.getReadAt() != null)
            .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toInstant() : OffsetDateTime.now().toInstant())
            .build();
    }
}
