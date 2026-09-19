package com.example.jari.issue.service;

import com.example.jari.issue.dto.WatchersResponse;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueWatcher;
import com.example.jari.issue.entity.IssueWatcherId;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.IssueWatcherRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueWatcherService {

    private final IssueWatcherRepository issueWatcherRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public WatchersResponse getWatchers(UUID issueId, UUID currentUserId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        List<IssueWatcher> watchers = issueWatcherRepository.findByIdIssueId(issueId);
        boolean isWatching = currentUserId != null && watchers.stream()
            .anyMatch(w -> w.getId().getUserId().equals(currentUserId));

        var userResponses = watchers.stream()
            .map(w -> userMapper.toResponse(w.getUser()))
            .toList();

        return WatchersResponse.builder()
            .count(watchers.size())
            .isWatching(isWatching)
            .watchers(userResponses)
            .build();
    }

    @Transactional
    public void watchIssue(UUID issueId, UUID currentUserId) {
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (!issueWatcherRepository.existsByIdIssueIdAndIdUserId(issueId, currentUserId)) {
            IssueWatcher watcher = IssueWatcher.builder()
                .id(new IssueWatcherId(issueId, currentUserId))
                .issue(issue)
                .user(user)
                .build();
            issueWatcherRepository.save(watcher);
        }
    }

    @Transactional
    public void unwatchIssue(UUID issueId, UUID currentUserId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }
        issueWatcherRepository.deleteById(new IssueWatcherId(issueId, currentUserId));
    }

    @Transactional(readOnly = true)
    public List<User> getWatcherUsers(UUID issueId) {
        return issueWatcherRepository.findByIdIssueId(issueId).stream()
            .map(IssueWatcher::getUser)
            .toList();
    }
}
