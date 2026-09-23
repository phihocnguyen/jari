package com.example.jari.issue.service;

import com.example.jari.issue.dto.CommentResponse;
import com.example.jari.issue.entity.Comment;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.CommentRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.IssueWatcherRepository;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueMapper mapper;

    @Mock
    private IssueWatcherRepository issueWatcherRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void list_usesFetchJoinQueryAndMapsComments() {
        UUID issueId = UUID.randomUUID();
        Comment comment = Comment.builder()
            .id(UUID.randomUUID())
            .author(User.builder().id(UUID.randomUUID()).displayName("Dev User").build())
            .content("Looks good")
            .build();
        CommentResponse response = CommentResponse.builder().content("Looks good").build();

        when(commentRepository.findByIssueIdWithAuthor(issueId)).thenReturn(List.of(comment));
        when(mapper.toCommentResponse(comment)).thenReturn(response);

        List<CommentResponse> result = commentService.list(issueId);

        assertThat(result).containsExactly(response);
        verify(commentRepository).findByIssueIdWithAuthor(issueId);
    }
}
