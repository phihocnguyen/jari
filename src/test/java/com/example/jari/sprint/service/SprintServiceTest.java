package com.example.jari.sprint.service;

import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.Status;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.issue.service.IssueHydrationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.ReadCacheEviction;
import com.example.jari.shared.exception.ApiException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.sprint.dto.AddIssueToSprintRequest;
import com.example.jari.sprint.dto.CreateSprintRequest;
import com.example.jari.sprint.dto.SprintResponse;
import com.example.jari.sprint.dto.UpdateSprintRequest;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintIssueId;
import com.example.jari.sprint.entity.SprintStatus;
import com.example.jari.sprint.mapper.SprintMapper;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
import com.example.jari.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock private SprintRepository sprintRepository;
    @Mock private SprintIssueRepository sprintIssueRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private SprintMapper sprintMapper;
    @Mock private IssueMapper issueMapper;
    @Mock private ReadCacheEviction readCacheEviction;
    @Mock private IssueHydrationService issueHydrationService;

    @InjectMocks private SprintService sprintService;

    private UUID projectId;
    private UUID sprintId;
    private Project project;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        sprintId = UUID.randomUUID();
        project = TestFixtures.project(projectId, TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")));
        sprint = TestFixtures.sprint(sprintId, project, SprintStatus.PLANNED);
    }

    @Test
    void create_savesPlannedSprint() {
        CreateSprintRequest req = new CreateSprintRequest();
        req.setName("Sprint 1");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintRepository.save(any())).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(SprintResponse.builder().id(sprintId).name("Sprint 1").build());

        SprintResponse response = sprintService.create(projectId, req);

        assertThat(response.getName()).isEqualTo("Sprint 1");
    }

    @Test
    void start_throwsWhenAnotherActiveSprintExists() {
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> sprintService.start(sprintId))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void complete_requiresActiveSprint() {
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.complete(sprintId))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void complete_marksSprintCompleted() {
        sprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(SprintResponse.builder().id(sprintId).build());

        sprintService.complete(sprintId);

        assertThat(sprint.getStatus()).isEqualTo(SprintStatus.COMPLETED);
        verify(readCacheEviction).evictBoard(projectId);
    }

    @Test
    void addIssue_movesIssueToSprint() {
        Issue issue = TestFixtures.issue(UUID.randomUUID(), project);
        AddIssueToSprintRequest req = new AddIssueToSprintRequest();
        req.setIssueId(issue.getId());
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprintId)).thenReturn(List.of());

        sprintService.addIssue(sprintId, req);

        verify(sprintIssueRepository).deleteByIssueId(issue.getId());
        verify(sprintIssueRepository).save(any(SprintIssue.class));
    }

    @Test
    void getBoard_requiresActiveSprint() {
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.getBoard(projectId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBoard_groupsIssuesByStatus() {
        sprint.setStatus(SprintStatus.ACTIVE);
        Issue issue = TestFixtures.issue(UUID.randomUUID(), project);
        Status status = TestFixtures.status(UUID.randomUUID(), "To Do", "TODO");
        issue.setStatus(status);
        SprintIssue si = TestFixtures.sprintIssue(sprint, issue, BigDecimal.TEN);

        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(Optional.of(sprint));
        when(sprintIssueRepository.findBySprintIdWithIssues(sprintId)).thenReturn(List.of(si));
        when(statusRepository.findAll()).thenReturn(List.of(status));
        when(issueMapper.toResponse(issue)).thenReturn(IssueResponse.builder().id(issue.getId()).build());

        var board = sprintService.getBoard(projectId);

        assertThat(board).hasSize(1);
        assertThat(board.get(0).getIssues()).hasSize(1);
        verify(issueHydrationService).hydrateCollections(anyList());
    }

    @Test
    void reorderIssues_noOpWhenEmpty() {
        sprintService.reorderIssues(sprintId, List.of());
        verify(sprintIssueRepository, never()).save(any());
    }

    @Test
    void update_updatesFields() {
        UpdateSprintRequest req = new UpdateSprintRequest();
        req.setName("Renamed");
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(SprintResponse.builder().id(sprintId).name("Renamed").build());

        SprintResponse response = sprintService.update(sprintId, req);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(sprint.getName()).isEqualTo("Renamed");
    }

    @Test
    void updateIssuePosition_updatesWhenPresent() {
        UUID issueId = UUID.randomUUID();
        SprintIssue si = SprintIssue.builder()
            .id(new SprintIssueId(sprintId, issueId))
            .sprint(sprint)
            .position(BigDecimal.ONE)
            .build();
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueRepository.findById(new SprintIssueId(sprintId, issueId))).thenReturn(Optional.of(si));

        sprintService.updateIssuePosition(sprintId, issueId, BigDecimal.valueOf(500));

        verify(sprintIssueRepository).save(si);
        assertThat(si.getPosition()).isEqualByComparingTo("500");
    }
}
