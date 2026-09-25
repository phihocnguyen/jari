package com.example.jari.automation.service;

import com.example.jari.automation.dto.RunAutomationRequest;
import com.example.jari.automation.entity.IssueAutomationLog;
import com.example.jari.automation.repository.IssueAutomationLogRepository;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.Status;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock private IssueAutomationLogRepository logRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StatusRepository statusRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AutomationService automationService;

    private Issue issue;
    private User actor;
    private Status inProgress;
    private Status done;

    @BeforeEach
    void setUp() {
        actor = TestFixtures.user(UUID.randomUUID(), "Actor");
        issue = TestFixtures.issue(UUID.randomUUID(), TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), actor)));
        inProgress = TestFixtures.status(UUID.randomUUID(), "In Progress", "IN_PROGRESS");
        done = TestFixtures.status(UUID.randomUUID(), "Done", "DONE");
    }

    @Test
    void listLogs_requiresExistingIssue() {
        UUID issueId = issue.getId();
        when(issueRepository.existsById(issueId)).thenReturn(false);

        assertThatThrownBy(() -> automationService.listLogs(issueId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onIssueStatusChanged_autoAssignsUnassignedInProgressIssue() {
        issue.setStatus(inProgress);
        issue.setAssignee(null);
        when(issueRepository.save(issue)).thenReturn(issue);
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        automationService.onIssueStatusChanged(issue, actor);

        assertThat(issue.getAssignee()).isEqualTo(actor);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void onIssueStatusChanged_closesParentWhenAllSubtasksDone() {
        Issue parent = TestFixtures.issue(UUID.randomUUID(), issue.getProject());
        parent.setStatus(inProgress);
        issue.setParent(parent);
        issue.setStatus(done);
        Issue sibling = TestFixtures.issue(UUID.randomUUID(), issue.getProject());
        sibling.setStatus(done);

        when(issueRepository.findByParentId(parent.getId())).thenReturn(List.of(issue, sibling));
        when(statusRepository.findByNameIgnoreCase("DONE")).thenReturn(Optional.of(done));
        when(issueRepository.save(parent)).thenReturn(parent);
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        automationService.onIssueStatusChanged(issue, actor);

        assertThat(parent.getStatus()).isEqualTo(done);
    }

    @Test
    void onIssueStatusChanged_noOpWhenIssueNull() {
        automationService.onIssueStatusChanged(null, actor);
        verify(issueRepository, never()).save(any());
    }

    @Test
    void runManualRule_autoAssignMe_assignsActor() {
        RunAutomationRequest req = new RunAutomationRequest();
        req.setRule("AUTO_ASSIGN_ME");
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(logRepository.save(any())).thenAnswer(inv -> {
            IssueAutomationLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        var response = automationService.runManualRule(issue.getId(), actor.getId(), req);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(issue.getAssignee()).isEqualTo(actor);
    }

    @Test
    void runManualRule_autoCloseParent_whenNoSubtasks() {
        RunAutomationRequest req = new RunAutomationRequest();
        req.setRule("AUTO_CLOSE_PARENT");
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(issueRepository.findByParentId(issue.getId())).thenReturn(List.of());
        when(logRepository.save(any())).thenAnswer(inv -> {
            IssueAutomationLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        var response = automationService.runManualRule(issue.getId(), null, req);

        assertThat(response.getStatus()).isEqualTo("TRIGGERED");
    }

    @Test
    void runManualRule_defaultRule_executesGenericDescription() {
        RunAutomationRequest req = new RunAutomationRequest();
        req.setRule("CUSTOM");
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(logRepository.save(any())).thenAnswer(inv -> {
            IssueAutomationLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        var response = automationService.runManualRule(issue.getId(), null, req);

        assertThat(response.getDescription()).contains("custom automation");
    }
}
