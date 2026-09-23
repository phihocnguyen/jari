package com.example.jari.issue.service;

import com.example.jari.issue.entity.IssueWatcher;
import com.example.jari.issue.entity.IssueWatcherId;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.IssueWatcherRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.dto.UserResponse;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueWatcherServiceTest {

    @Mock private IssueWatcherRepository issueWatcherRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private IssueWatcherService issueWatcherService;

    @Test
    void getWatchers_marksCurrentUserWatching() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var user = TestFixtures.user(userId, "Watcher");
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), user)));
        IssueWatcher watcher = IssueWatcher.builder()
            .id(new IssueWatcherId(issueId, userId))
            .issue(issue)
            .user(user)
            .build();

        when(issueRepository.existsById(issueId)).thenReturn(true);
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of(watcher));
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(userId).build());

        var response = issueWatcherService.getWatchers(issueId, userId);

        assertThat(response.isWatching()).isTrue();
        assertThat(response.getCount()).isEqualTo(1);
    }

    @Test
    void getWatchers_notWatchingWhenAnonymous() {
        UUID issueId = UUID.randomUUID();
        when(issueRepository.existsById(issueId)).thenReturn(true);
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        var response = issueWatcherService.getWatchers(issueId, null);

        assertThat(response.isWatching()).isFalse();
    }

    @Test
    void watchIssue_isIdempotent() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var user = TestFixtures.user(userId, "Watcher");
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), user)));

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, userId)).thenReturn(true);

        issueWatcherService.watchIssue(issueId, userId);

        verify(issueWatcherRepository, never()).save(any());
    }

    @Test
    void watchIssue_savesNewWatcher() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var user = TestFixtures.user(userId, "Watcher");
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), user)));

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, userId)).thenReturn(false);

        issueWatcherService.watchIssue(issueId, userId);

        verify(issueWatcherRepository).save(any(IssueWatcher.class));
    }

    @Test
    void unwatchIssue_requiresExistingIssue() {
        UUID issueId = UUID.randomUUID();
        when(issueRepository.existsById(issueId)).thenReturn(false);

        assertThatThrownBy(() -> issueWatcherService.unwatchIssue(issueId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getWatcherUsers_returnsUsers() {
        UUID issueId = UUID.randomUUID();
        var user = TestFixtures.user(UUID.randomUUID(), "Watcher");
        IssueWatcher watcher = IssueWatcher.builder()
            .id(new IssueWatcherId(issueId, user.getId()))
            .user(user)
            .build();
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of(watcher));

        assertThat(issueWatcherService.getWatcherUsers(issueId)).containsExactly(user);
    }
}
