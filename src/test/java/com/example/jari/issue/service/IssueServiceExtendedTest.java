package com.example.jari.issue.service;

import com.example.jari.automation.service.AutomationService;
import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.dto.UpdateIssueRequest;
import com.example.jari.issue.entity.*;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.*;
import com.example.jari.issue.search.IssueSearchService;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.ReadCacheEviction;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceExtendedTest {

    @Mock private IssueRepository issueRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private IssueTypeRepository issueTypeRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private PriorityRepository priorityRepository;
    @Mock private IssueHistoryRepository historyRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private SprintIssueRepository sprintIssueRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private LabelRepository labelRepository;
    @Mock private com.example.jari.release.repository.ReleaseRepository releaseRepository;
    @Mock private com.example.jari.component.repository.ComponentRepository componentRepository;
    @Mock private IssueHistoryService historyService;
    @Mock private IssueMapper mapper;
    @Mock private NotificationService notificationService;
    @Mock private IssueWatcherRepository issueWatcherRepository;
    @Mock private AutomationService automationService;
    @Mock private IssueSearchService issueSearchService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReadCacheEviction readCacheEviction;
    @Mock private IssueHydrationService issueHydrationService;

    @InjectMocks private IssueService issueService;

    private UUID issueId;
    private UUID actorId;
    private User actor;
    private User assignee;
    private Issue issue;
    private Status todo;
    private Status inProgress;

    @BeforeEach
    void setUp() {
        issueId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        actor = TestFixtures.user(actorId, "Actor");
        assignee = TestFixtures.user(UUID.randomUUID(), "Assignee");
        Project project = TestFixtures.project(UUID.randomUUID(), TestFixtures.workspace(UUID.randomUUID(), actor));
        issue = TestFixtures.issue(issueId, project);
        issue.setReporter(actor);
        issue.setAssignee(assignee);
        todo = TestFixtures.status(UUID.randomUUID(), "To Do", "TODO");
        inProgress = TestFixtures.status(UUID.randomUUID(), "In Progress", "IN_PROGRESS");
        issue.setStatus(todo);
        issue.setPriority(TestFixtures.priority(UUID.randomUUID(), "HIGH"));
        issue.setIssueType(TestFixtures.issueType(UUID.randomUUID(), "Story"));
    }

    @Test
    void update_notifiesWhenAssigneeChanges() {
        User newAssignee = TestFixtures.user(UUID.randomUUID(), "New");
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setAssigneeId(newAssignee.getId());
        stubUpdateActor();
        when(userRepository.findById(newAssignee.getId())).thenReturn(Optional.of(newAssignee));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.update(issueId, actorId, req);

        verify(notificationService).notifyIssueAssigned(issue, actor, newAssignee);
    }

    @Test
    void update_movesIssueToSprint() {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = TestFixtures.sprint(sprintId, issue.getProject(), com.example.jari.sprint.entity.SprintStatus.ACTIVE);
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setSprintId(sprintId);
        stubUpdateActor();
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(issueRepository.saveAndFlush(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.update(issueId, actorId, req);

        verify(issueRepository).saveAndFlush(issue);
        verify(sprintRepository).findById(sprintId);
    }

    @Test
    void update_changesStatusByName() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setStatus("In Progress");
        stubUpdateActor();
        when(statusRepository.findByName("In Progress")).thenReturn(Optional.of(inProgress));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.update(issueId, actorId, req);

        verify(historyService).record(issue, actor, "status", todo.getName(), inProgress.getName());
    }

    @Test
    void setRelease_assignsRelease() {
        UUID releaseId = UUID.randomUUID();
        var release = com.example.jari.release.entity.Release.builder().id(releaseId).name("v1").build();
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.setRelease(issueId, releaseId);

        assert issue.getRelease() == release;
    }

    @Test
    void updateSprint_movesIssueToNewSprint() {
        UUID oldSprintId = UUID.randomUUID();
        UUID newSprintId = UUID.randomUUID();
        Sprint oldSprint = TestFixtures.sprint(oldSprintId, issue.getProject(), com.example.jari.sprint.entity.SprintStatus.ACTIVE);
        Sprint newSprint = TestFixtures.sprint(newSprintId, issue.getProject(), com.example.jari.sprint.entity.SprintStatus.PLANNED);
        issue.getSprintIssues().add(TestFixtures.sprintIssue(oldSprint, issue, BigDecimal.TEN));

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(issueRepository.saveAndFlush(issue)).thenReturn(issue);
        when(sprintRepository.findById(newSprintId)).thenReturn(Optional.of(newSprint));
        when(sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(newSprintId)).thenReturn(List.of());
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateSprint(issueId, actorId, newSprintId);

        verify(historyService).record(eq(issue), eq(actor), eq("sprint"), any(), eq(newSprint.getName()));
        verify(readCacheEviction).evictBoard(issue.getProject().getId());
    }

    @Test
    void notifyWatchers_swallowsNotificationErrors() {
        User watcherUser = TestFixtures.user(UUID.randomUUID(), "Watcher");
        IssueWatcher watcher = IssueWatcher.builder()
            .id(new IssueWatcherId(issueId, watcherUser.getId()))
            .issue(issue)
            .user(watcherUser)
            .build();
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setStatusId(inProgress.getId());
        stubUpdateActor();
        when(statusRepository.findById(inProgress.getId())).thenReturn(Optional.of(inProgress));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of(watcher));
        doThrow(new RuntimeException("notify failed")).when(notificationService)
            .notifyIssueUpdated(eq(issue), eq(actor), eq(watcherUser));

        issueService.updateStatus(issueId, actorId, req);

        verify(automationService).onIssueStatusChanged(issue, actor);
    }

    private void stubUpdateActor() {
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
    }
}
