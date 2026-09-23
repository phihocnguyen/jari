package com.example.jari.issue.service;

import com.example.jari.issue.dto.IssueHistoryResponse;
import com.example.jari.issue.entity.IssueHistory;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueHistoryRepository;
import com.example.jari.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueHistoryServiceTest {

    @Mock private IssueHistoryRepository historyRepository;
    @Mock private IssueMapper mapper;
    @InjectMocks private IssueHistoryService issueHistoryService;

    @Test
    @Transactional
    void record_skipsWhenValuesEqual() {
        var issue = TestFixtures.issue(UUID.randomUUID(), TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Dev"))));
        var actor = TestFixtures.user(UUID.randomUUID(), "Actor");

        issueHistoryService.record(issue, actor, "status", "Open", "Open");

        verify(historyRepository, never()).save(any());
    }

    @Test
    @Transactional
    void record_persistsWhenValuesDiffer() {
        var issue = TestFixtures.issue(UUID.randomUUID(), TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Dev"))));
        var actor = TestFixtures.user(UUID.randomUUID(), "Actor");

        issueHistoryService.record(issue, actor, "status", "Open", "Done");

        verify(historyRepository).save(any(IssueHistory.class));
    }

    @Test
    void getHistory_mapsResponses() {
        UUID issueId = UUID.randomUUID();
        IssueHistory history = IssueHistory.builder().field("status").newValue("Done").build();
        when(historyRepository.findByIssueIdWithUser(issueId)).thenReturn(List.of(history));
        when(mapper.toHistoryResponse(history)).thenReturn(IssueHistoryResponse.builder().field("status").build());

        issueHistoryService.getHistory(issueId);

        verify(mapper).toHistoryResponse(history);
    }
}
