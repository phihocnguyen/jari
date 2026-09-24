package com.example.jari.issue.service;

import com.example.jari.issue.dto.CommentEvent;
import com.example.jari.issue.dto.CommentRequest;
import com.example.jari.issue.dto.CommentResponse;
import com.example.jari.issue.entity.Comment;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueWatcher;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.CommentRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.IssueWatcherRepository;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final IssueMapper mapper;
    private final IssueWatcherRepository issueWatcherRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID issueId) {
        return commentRepository.findByIssueIdWithAuthor(issueId).stream()
            .map(mapper::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse create(UUID issueId, UUID authorId, CommentRequest req) {
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", authorId));
        Comment comment = commentRepository.save(Comment.builder()
            .issue(issue).author(author).content(req.getContent()).build());

        notifyCommentRecipients(issue, author);
        CommentResponse response = mapper.toCommentResponse(comment);
        broadcast(issueId, CommentEvent.builder()
            .type("CREATED")
            .issueId(issueId)
            .commentId(response.getId())
            .comment(response)
            .build());
        return response;
    }

    @Transactional
    public CommentResponse update(UUID commentId, UUID requesterId, CommentRequest req) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        if (!comment.getAuthor().getId().equals(requesterId)) throw new ForbiddenException();
        comment.setContent(req.getContent());
        Comment saved = commentRepository.save(comment);
        CommentResponse response = mapper.toCommentResponse(saved);
        UUID issueId = saved.getIssue().getId();
        broadcast(issueId, CommentEvent.builder()
            .type("UPDATED")
            .issueId(issueId)
            .commentId(response.getId())
            .comment(response)
            .build());
        return response;
    }

    @Transactional
    public void delete(UUID commentId, UUID requesterId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        if (!comment.getAuthor().getId().equals(requesterId)) throw new ForbiddenException();
        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
        UUID issueId = comment.getIssue().getId();
        broadcast(issueId, CommentEvent.builder()
            .type("DELETED")
            .issueId(issueId)
            .commentId(commentId)
            .build());
    }

    /**
     * Notify watchers + assignee + reporter (deduped). Author never gets a self-notification
     * ({@link NotificationService#notifyIssueCommented} also skips self).
     */
    private void notifyCommentRecipients(Issue issue, User author) {
        Map<UUID, User> recipients = new LinkedHashMap<>();

        List<IssueWatcher> watchers = issueWatcherRepository.findByIdIssueId(issue.getId());
        for (IssueWatcher watcher : watchers) {
            if (watcher.getUser() != null) {
                recipients.putIfAbsent(watcher.getUser().getId(), watcher.getUser());
            }
        }
        if (issue.getAssignee() != null) {
            recipients.putIfAbsent(issue.getAssignee().getId(), issue.getAssignee());
        }
        if (issue.getReporter() != null) {
            recipients.putIfAbsent(issue.getReporter().getId(), issue.getReporter());
        }

        for (User recipient : recipients.values()) {
            notificationService.notifyIssueCommented(issue, author, recipient);
        }
    }

    private void broadcast(UUID issueId, CommentEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/issues/" + issueId + "/comments", event);
        } catch (Exception e) {
            log.warn("Failed to broadcast comment event for issue {}: {}", issueId, e.getMessage());
        }
    }
}
