package com.example.jari.automation.service;

import com.example.jari.automation.dto.RunAutomationRequest;
import com.example.jari.automation.entity.IssueAutomationLog;
import com.example.jari.automation.repository.IssueAutomationLogRepository;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.Status;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceExtendedTest {

    @Mock private IssueAutomationLogRepository logRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StatusRepository statusRepository;
    @Mock private com.example.jari.user.repository.UserRepository userRepository;
    @InjectMocks private AutomationService automationService;

    private Issue parent;
    private Status done;

    @BeforeEach
    void setUp() {
        User actor = TestFixtures.user(UUID.randomUUID(), "Actor");
        parent = TestFixtures.issue(UUID.randomUUID(), TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), actor)));
        done = TestFixtures.status(UUID.randomUUID(), "Done", "DONE");
    }

    @Test
    void runManualRule_autoCloseParent_closesWhenAllSubtasksDone() {
        Issue subtask = TestFixtures.issue(UUID.randomUUID(), parent.getProject());
        subtask.setStatus(done);
        RunAutomationRequest req = new RunAutomationRequest();
        req.setRule("AUTO_CLOSE_PARENT");

        when(issueRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(issueRepository.findByParentId(parent.getId())).thenReturn(List.of(subtask));
        when(statusRepository.findByNameIgnoreCase("DONE")).thenReturn(Optional.of(done));
        when(issueRepository.save(parent)).thenReturn(parent);
        when(logRepository.save(any())).thenAnswer(inv -> {
            IssueAutomationLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        var response = automationService.runManualRule(parent.getId(), null, req);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(parent.getStatus()).isEqualTo(done);
    }

    @Test
    void runManualRule_autoAssignMe_failsWithoutActor() {
        RunAutomationRequest req = new RunAutomationRequest();
        req.setRule("AUTO_ASSIGN_ME");
        when(issueRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(logRepository.save(any())).thenAnswer(inv -> {
            IssueAutomationLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        var response = automationService.runManualRule(parent.getId(), null, req);

        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void onIssueStatusChanged_matchesInProgressByName() {
        Status status = TestFixtures.status(UUID.randomUUID(), "IN PROGRESS", "OTHER");
        Issue issue = TestFixtures.issue(UUID.randomUUID(), parent.getProject());
        User actor = TestFixtures.user(UUID.randomUUID(), "Actor");
        issue.setStatus(status);
        issue.setAssignee(null);
        when(issueRepository.save(issue)).thenReturn(issue);
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        automationService.onIssueStatusChanged(issue, actor);

        assertThat(issue.getAssignee()).isEqualTo(actor);
    }

    @Test
    void recordLog_swallowsPersistenceErrors() {
        Issue issue = TestFixtures.issue(UUID.randomUUID(), parent.getProject());
        when(logRepository.save(any())).thenThrow(new RuntimeException("db down"));

        automationService.recordLog(issue, "rule", "FAILED", "desc");

        verify(logRepository).save(any());
    }
}
