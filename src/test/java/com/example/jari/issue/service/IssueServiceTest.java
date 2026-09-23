package com.example.jari.issue.service;

import com.example.jari.automation.service.AutomationService;
import com.example.jari.issue.dto.*;
import com.example.jari.issue.entity.*;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.*;
import com.example.jari.issue.search.IssueSearchService;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.ReadCacheEviction;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.shared.response.PageResponse;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintIssueId;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

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

    @InjectMocks
    private IssueService issueService;

    private UUID projectId;
    private UUID issueId;
    private Project project;
    private Issue issue;
    private User reporter;
    private User assignee;
    private Status todo;
    private Status done;
    private Priority high;
    private IssueType story;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        issueId = UUID.randomUUID();
        reporter = TestFixtures.user(UUID.randomUUID(), "Reporter");
        assignee = TestFixtures.user(UUID.randomUUID(), "Assignee");
        project = TestFixtures.project(projectId, TestFixtures.workspace(UUID.randomUUID(), reporter));
        issue = TestFixtures.issue(issueId, project);
        issue.setReporter(reporter);
        todo = TestFixtures.status(UUID.randomUUID(), "To Do", "TODO");
        done = TestFixtures.status(UUID.randomUUID(), "Done", "DONE");
        high = TestFixtures.priority(UUID.randomUUID(), "HIGH");
        story = TestFixtures.issueType(UUID.randomUUID(), "Story");
        issue.setStatus(todo);
        issue.setPriority(high);
        issue.setIssueType(story);
    }

    @Test
    void get_hydratesIssueBeforeMapping() {
        IssueResponse response = IssueResponse.builder().id(issueId).title("Demo").build();
        when(issueRepository.findDetailedById(issueId)).thenReturn(Optional.of(issue));
        when(issueHydrationService.hydrateCollections(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(response);

        assertThat(issueService.get(issueId)).isEqualTo(response);
    }

    @Test
    void getByIdOrKey_usesUuidWhenValid() {
        when(issueRepository.findDetailedById(issueId)).thenReturn(Optional.of(issue));
        when(issueHydrationService.hydrateCollections(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.getByIdOrKey(issueId.toString());

        verify(issueRepository).findDetailedById(issueId);
    }

    @Test
    void getByIdOrKey_fallsBackToIssueKey() {
        when(issueRepository.findDetailedByIssueKeyIgnoreCase("PROJ-1")).thenReturn(Optional.of(issue));
        when(issueHydrationService.hydrateCollections(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.getByIdOrKey("PROJ-1");

        verify(issueRepository).findDetailedByIssueKeyIgnoreCase("PROJ-1");
    }

    @Test
    void create_persistsIssueAndNotifiesAssignee() {
        UUID typeId = story.getId();
        UUID statusId = todo.getId();
        UUID priorityId = high.getId();
        CreateIssueRequest req = new CreateIssueRequest();
        req.setTitle("New");
        req.setIssueTypeId(typeId);
        req.setStatusId(statusId);
        req.setPriorityId(priorityId);
        req.setAssigneeId(assignee.getId());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.findMaxIssueNumber(projectId)).thenReturn(0);
        when(issueTypeRepository.findById(typeId)).thenReturn(Optional.of(story));
        when(statusRepository.findById(statusId)).thenReturn(Optional.of(todo));
        when(priorityRepository.findById(priorityId)).thenReturn(Optional.of(high));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> {
            Issue saved = inv.getArgument(0);
            saved.setId(issueId);
            return saved;
        });
        when(issueHydrationService.hydrateCollections(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.create(projectId, reporter.getId(), req);

        verify(notificationService).notifyIssueAssigned(any(), eq(reporter), eq(assignee));
        verify(eventPublisher).publishEvent(any(Object.class));
        verify(readCacheEviction).evictIssue(eq(issueId), anyString(), eq(projectId));
    }

    @Test
    void create_withSprint_addsSprintIssue() {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = TestFixtures.sprint(sprintId, project, com.example.jari.sprint.entity.SprintStatus.PLANNED);
        CreateIssueRequest req = baseCreateRequest();
        req.setSprintId(sprintId);
        stubCreateDependencies();

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(issueRepository.save(any())).thenAnswer(inv -> {
            Issue saved = inv.getArgument(0);
            saved.setId(issueId);
            return saved;
        });
        when(issueHydrationService.hydrateCollections(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.create(projectId, reporter.getId(), req);

        verify(sprintIssueRepository).save(any(SprintIssue.class));
    }

    @Test
    void list_returnsSearchResultWhenAvailable() {
        PageResponse<IssueResponse> searched = PageResponse.of(new PageImpl<>(List.of()));
        when(issueSearchService.search(eq(projectId), any())).thenReturn(searched);

        assertThat(issueService.list(projectId, new IssueFilterRequest())).isEqualTo(searched);
        verify(issueRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void list_fallsBackToJpaWhenSearchUnavailable() {
        IssueFilterRequest filter = new IssueFilterRequest();
        filter.setPage(0);
        filter.setSize(20);
        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueSearchService.search(projectId, filter)).thenReturn(null);
        when(issueRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        PageResponse<IssueResponse> result = issueService.list(projectId, filter);

        assertThat(result.getData()).hasSize(1);
        verify(issueHydrationService).hydrateCollections(page.getContent());
    }

    @Test
    void updateStatus_recordsHistoryWhenChanged() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setStatusId(done.getId());
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(statusRepository.findById(done.getId())).thenReturn(Optional.of(done));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.updateStatus(issueId, reporter.getId(), req);

        verify(historyService).record(issue, reporter, "status", todo.getName(), done.getName());
        verify(automationService).onIssueStatusChanged(issue, reporter);
    }

    @Test
    void updateAssignee_notifiesWhenChanged() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setAssigneeId(assignee.getId());
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.updateAssignee(issueId, reporter.getId(), req);

        verify(notificationService).notifyIssueAssigned(issue, reporter, assignee);
    }

    @Test
    void updateAssignee_clearsAssigneeWithoutNotification() {
        issue.setAssignee(assignee);
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setAssigneeId(null);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.updateAssignee(issueId, reporter.getId(), req);

        verify(notificationService, never()).notifyIssueAssigned(any(), any(), any());
        assertThat(issue.getAssignee()).isNull();
    }

    @Test
    void updateParent_rejectsSelfParent() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setParentId(issueId);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueService.updateParent(issueId, reporter.getId(), req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("own parent");
    }

    @Test
    void updateParent_rejectsCycle() {
        UUID parentId = UUID.randomUUID();
        Issue parent = TestFixtures.issue(parentId, project);
        parent.setParent(issue);
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setParentId(parentId);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> issueService.updateParent(issueId, reporter.getId(), req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("descendant");
    }

    @Test
    void setLabels_replacesExistingLabels() {
        Label label = Label.builder().id(UUID.randomUUID()).name("bug").build();
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findAllById(List.of(label.getId()))).thenReturn(List.of(label));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.setLabels(issueId, List.of(label.getId()));

        assertThat(issue.getLabels()).containsExactly(label);
    }

    @Test
    void setRelease_clearsWhenNull() {
        issue.setRelease(com.example.jari.release.entity.Release.builder().id(UUID.randomUUID()).build());
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.setRelease(issueId, null);

        assertThat(issue.getRelease()).isNull();
    }

    @Test
    void updateSprint_returnsEarlyWhenUnchanged() {
        UUID sprintId = UUID.randomUUID();
        Sprint sprint = TestFixtures.sprint(sprintId, project, com.example.jari.sprint.entity.SprintStatus.ACTIVE);
        SprintIssue si = TestFixtures.sprintIssue(sprint, issue, BigDecimal.TEN);
        issue.getSprintIssues().add(si);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateSprint(issueId, null, sprintId);

        verify(sprintRepository, never()).findById(any());
        verify(historyService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void delete_cleansRelatedData() {
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        issueService.delete(issueId);

        verify(commentRepository).deleteByIssueId(issueId);
        verify(historyRepository).deleteByIssueId(issueId);
        verify(issueRepository).delete(issue);
    }

    @Test
    void reorderIssues_noOpWhenEmpty() {
        issueService.reorderIssues(projectId, null);
        issueService.reorderIssues(projectId, List.of());
        verify(issueRepository, never()).updatePosition(any(), any());
    }

    @Test
    void reorderIssues_updatesPositions() {
        UUID id1 = UUID.randomUUID();
        issueService.reorderIssues(projectId, List.of(id1));

        verify(issueRepository).updatePosition(id1, BigDecimal.valueOf(1000));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void resolveStatusByNameOrId_usesIdWhenProvided() {
        when(statusRepository.findById(todo.getId())).thenReturn(Optional.of(todo));

        assertThat(issueService.resolveStatusByNameOrId(null, todo.getId())).isEqualTo(todo);
    }

    @Test
    void resolveStatusByNameOrId_matchesNameAndUnderscoreVariant() {
        when(statusRepository.findByName("In Progress")).thenReturn(Optional.of(todo));

        assertThat(issueService.resolveStatusByNameOrId("In Progress", null)).isEqualTo(todo);
    }

    @Test
    void resolveStatusByNameOrId_matchesUnderscoreAlias() {
        when(statusRepository.findByName("IN_PROGRESS")).thenReturn(Optional.empty());
        when(statusRepository.findByName("IN PROGRESS")).thenReturn(Optional.of(todo));

        assertThat(issueService.resolveStatusByNameOrId("IN_PROGRESS", null)).isEqualTo(todo);
    }

    @Test
    void resolvePriorityByNameOrId_fallsBackToFirstPriority() {
        when(priorityRepository.findByName("URGENT")).thenReturn(Optional.empty());
        when(priorityRepository.findAll()).thenReturn(List.of(high));

        assertThat(issueService.resolvePriorityByNameOrId("URGENT", null)).isEqualTo(high);
    }

    @Test
    void resolveStatusByNameOrId_throwsWhenNothingFound() {
        when(statusRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> issueService.resolveStatusByNameOrId("missing", null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliesPartialFieldsAndNotifiesDueDate() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setTitle("Updated");
        req.setDescription("Desc");
        req.setDueDate(LocalDate.now().plusDays(3));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.update(issueId, reporter.getId(), req);

        verify(historyService).record(issue, reporter, "title", "Issue", "Updated");
        verify(notificationService).notifyIssueDueSoon(issue, issue.getAssignee());
    }

    @Test
    void update_skipsStatusHistoryWhenUnchanged() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setStatusId(todo.getId());
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(statusRepository.findById(todo.getId())).thenReturn(Optional.of(todo));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.update(issueId, reporter.getId(), req);

        verify(historyService, never()).record(eq(issue), eq(reporter), eq("status"), any(), any());
    }

    @Test
    void updatePriority_recordsHistoryWhenChanged() {
        Priority low = TestFixtures.priority(UUID.randomUUID(), "LOW");
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setPriorityId(low.getId());
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(priorityRepository.findById(low.getId())).thenReturn(Optional.of(low));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());
        when(issueWatcherRepository.findByIdIssueId(issueId)).thenReturn(List.of());

        issueService.updatePriority(issueId, reporter.getId(), req);

        verify(historyService).record(issue, reporter, "priority", high.getName(), low.getName());
    }

    @Test
    void updateDates_clearsAndSetsDates() {
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setStartDate(LocalDate.now());
        req.setDueDate(null);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateDates(issueId, req);

        assertThat(issue.getStartDate()).isEqualTo(req.getStartDate());
        assertThat(issue.getDueDate()).isNull();
    }

    @Test
    void updateParent_allowsClearingParent() {
        Issue parent = TestFixtures.issue(UUID.randomUUID(), project);
        issue.setParent(parent);
        UpdateIssueRequest req = new UpdateIssueRequest();
        req.setParentId(null);
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.updateParent(issueId, reporter.getId(), req);

        assertThat(issue.getParent()).isNull();
    }

    @Test
    void setComponents_replacesComponents() {
        var component = com.example.jari.component.entity.Component.builder().id(UUID.randomUUID()).name("API").build();
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(componentRepository.findAllById(List.of(component.getId()))).thenReturn(List.of(component));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(mapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issueId).build());

        issueService.setComponents(issueId, List.of(component.getId()));

        assertThat(issue.getComponents()).containsExactly(component);
    }

    @Test
    void resolveStatusByNameOrId_matchesCategory() {
        when(statusRepository.findByName("DONE")).thenReturn(Optional.empty());
        when(statusRepository.findAll()).thenReturn(List.of(done));

        assertThat(issueService.resolveStatusByNameOrId("DONE", null)).isEqualTo(done);
    }

    @Test
    void resolvePriorityByNameOrId_usesDefaultWhenBlank() {
        when(priorityRepository.findAll()).thenReturn(List.of(high));

        assertThat(issueService.resolvePriorityByNameOrId(null, null)).isEqualTo(high);
    }

    @Test
    void get_throwsWhenMissing() {
        when(issueRepository.findDetailedById(issueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.get(issueId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateIssueRequest baseCreateRequest() {
        CreateIssueRequest req = new CreateIssueRequest();
        req.setTitle("New");
        req.setIssueTypeId(story.getId());
        req.setStatusId(todo.getId());
        req.setPriorityId(high.getId());
        return req;
    }

    private void stubCreateDependencies() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(issueRepository.findMaxIssueNumber(projectId)).thenReturn(1);
        when(issueTypeRepository.findById(story.getId())).thenReturn(Optional.of(story));
        when(statusRepository.findById(todo.getId())).thenReturn(Optional.of(todo));
        when(priorityRepository.findById(high.getId())).thenReturn(Optional.of(high));
    }
}
