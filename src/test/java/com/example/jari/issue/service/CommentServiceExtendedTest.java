package com.example.jari.issue.service;

import com.example.jari.issue.dto.CommentRequest;
import com.example.jari.issue.dto.CommentResponse;
import com.example.jari.issue.entity.Comment;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueWatcher;
import com.example.jari.issue.entity.IssueWatcherId;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.CommentRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.IssueWatcherRepository;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceExtendedTest {

    @Mock private CommentRepository commentRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private UserRepository userRepository;
    @Mock private IssueMapper mapper;
    @Mock private IssueWatcherRepository issueWatcherRepository;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private CommentService commentService;

    private UUID issueId;
    private UUID authorId;
    private User author;
    private Issue issue;

    @BeforeEach
    void setUp() {
        issueId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        author = TestFixtures.user(authorId, "Author");
        issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), author)));
        issue.setReporter(author);
    }

    @Test
    void create_notifiesWatchersAssigneeAndReporter_andBroadcasts() {
        User watcherUser = TestFixtures.user(UUID.randomUUID(), "Watcher");
        User assignee = TestFixtures.user(UUID.randomUUID(), "Assignee");
        User reporter = TestFixtures.user(UUID.randomUUID(), "Reporter");
        issue.setAssignee(assignee);
        issue.setReporter(reporter);

        CommentRequest req = new CommentRequest();
        req.setContent("Nice");
        UUID commentId = UUID.randomUUID();
        Comment saved = Comment.builder().id(commentId).issue(issue).author(author).content("Nice").build();
        CommentResponse response = CommentResponse.builder().id(commentId).content("Nice").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.save(any())).thenReturn(saved);
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of(
            IssueWatcher.builder().id(new IssueWatcherId(issueId, watcherUser.getId())).issue(issue).user(watcherUser).build()
        ));
        when(mapper.toCommentResponse(saved)).thenReturn(response);

        commentService.create(issueId, authorId, req);

        verify(notificationService).notifyIssueCommented(issue, author, watcherUser);
        verify(notificationService).notifyIssueCommented(issue, author, assignee);
        verify(notificationService).notifyIssueCommented(issue, author, reporter);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/issues/" + issueId + "/comments"), payload.capture());
        assertThat(payload.getValue().toString()).contains("CREATED");
    }

    @Test
    void create_dedupesWhenAssigneeAlsoWatches() {
        User assignee = TestFixtures.user(UUID.randomUUID(), "Assignee");
        issue.setAssignee(assignee);
        issue.setReporter(null);

        CommentRequest req = new CommentRequest();
        req.setContent("Hi");
        Comment saved = Comment.builder().id(UUID.randomUUID()).issue(issue).author(author).content("Hi").build();
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.save(any())).thenReturn(saved);
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of(
            IssueWatcher.builder().id(new IssueWatcherId(issueId, assignee.getId())).issue(issue).user(assignee).build()
        ));
        when(mapper.toCommentResponse(saved)).thenReturn(CommentResponse.builder().content("Hi").build());

        commentService.create(issueId, authorId, req);

        verify(notificationService, times(1)).notifyIssueCommented(issue, author, assignee);
    }

    @Test
    void update_rejectsNonAuthor() {
        Comment comment = Comment.builder().id(UUID.randomUUID()).author(author).content("Old").build();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(comment.getId(), UUID.randomUUID(), new CommentRequest()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_softDeletesForAuthor() {
        Comment comment = Comment.builder().id(UUID.randomUUID()).author(author).issue(issue).content("Old").build();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        commentService.delete(comment.getId(), authorId);

        assertThat(comment.getDeletedAt()).isNotNull();
        verify(messagingTemplate).convertAndSend(eq("/topic/issues/" + issueId + "/comments"), any(Object.class));
    }

    @Test
    void update_allowsAuthorToEditComment() {
        Comment comment = Comment.builder().id(UUID.randomUUID()).author(author).issue(issue).content("Old").build();
        CommentRequest req = new CommentRequest();
        req.setContent("Updated");
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(mapper.toCommentResponse(comment)).thenReturn(CommentResponse.builder().content("Updated").build());

        commentService.update(comment.getId(), authorId, req);

        assertThat(comment.getContent()).isEqualTo("Updated");
        verify(messagingTemplate).convertAndSend(eq("/topic/issues/" + issueId + "/comments"), any(Object.class));
    }

    @Test
    void create_throwsWhenIssueMissing() {
        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(issueId, authorId, new CommentRequest()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
