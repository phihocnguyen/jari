package com.example.jari.issue.service;

import com.example.jari.issue.dto.IssueHistoryResponse;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueHistory;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueHistoryRepository;
import com.example.jari.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueHistoryService {

    private final IssueHistoryRepository historyRepository;
    private final IssueMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Issue issue, User actor, String field, String oldValue, String newValue) {
        if (java.util.Objects.equals(oldValue, newValue)) return;
        historyRepository.save(IssueHistory.builder()
            .issue(issue)
            .user(actor)
            .field(field)
            .oldValue(oldValue)
            .newValue(newValue)
            .build());
    }

    @Transactional(readOnly = true)
    public List<IssueHistoryResponse> getHistory(UUID issueId) {
        return historyRepository.findByIssueIdWithUser(issueId).stream()
            .map(mapper::toHistoryResponse).toList();
    }
}
