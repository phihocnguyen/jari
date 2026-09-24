package com.example.jari.report.service;

import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.report.dto.ProjectReportsResponse;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintStatus;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
import com.example.jari.support.TestFixtures;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private SprintIssueRepository sprintIssueRepository;
    @Mock private EntityManager em;

    @InjectMocks private ReportService reportService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        ReflectionTestUtils.setField(reportService, "em", em);
        stubEmptyEmQueries();
    }

    @Test
    void getReports_throwsWhenProjectMissing() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReports(projectId, null, null, null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReports_returnsEmptyBurndownWhenNoSprint() {
        var project = TestFixtures.project(projectId,
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner")));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of());

        ProjectReportsResponse reports = reportService.getReports(projectId, null, 7, 4);

        assertThat(reports.getBurndown().getMessage()).contains("No active or completed sprint");
        assertThat(reports.getVelocity().getSprints()).isEmpty();
        assertThat(reports.getCumulativeFlow().getPoints()).hasSize(7);
        assertThat(reports.getCreatedVsResolved().getPoints()).hasSize(7);
    }

    @Test
    void getReports_buildsBurndownPointsForDatedSprint() {
        var project = TestFixtures.project(projectId,
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner")));
        Sprint sprint = TestFixtures.sprint(UUID.randomUUID(), project, SprintStatus.ACTIVE);
        sprint.setStartDate(OffsetDateTime.now().minusDays(2));
        sprint.setEndDate(OffsetDateTime.now().plusDays(2));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE))
            .thenReturn(Optional.of(sprint));
        when(sprintIssueRepository.findBySprintIdWithIssues(sprint.getId())).thenReturn(List.of());
        when(sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(sprint));

        ProjectReportsResponse reports = reportService.getReports(projectId, null, 5, 4);

        assertThat(reports.getBurndown().getSprintId()).isEqualTo(sprint.getId());
        assertThat(reports.getBurndown().getPoints()).isNotEmpty();
        assertThat(reports.getVelocity().getSprints()).hasSize(1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubEmptyEmQueries() {
        Query untyped = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(untyped);
        when(untyped.setParameter(anyString(), any())).thenReturn(untyped);
        when(untyped.getResultList()).thenReturn(List.of());

        TypedQuery typed = mock(TypedQuery.class);
        when(em.createQuery(anyString(), any(Class.class))).thenReturn(typed);
        when(typed.setParameter(anyString(), any())).thenReturn(typed);
        when(typed.getResultList()).thenReturn(List.of());
    }
}
