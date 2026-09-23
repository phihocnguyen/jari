package com.example.jari.sprint.service;

import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.issue.service.IssueHydrationService;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.ReadCacheEviction;
import com.example.jari.sprint.dto.SprintResponse;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintIssueId;
import com.example.jari.sprint.entity.SprintStatus;
import com.example.jari.sprint.mapper.SprintMapper;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
import com.example.jari.support.TestFixtures;
import com.example.jari.issue.mapper.IssueMapper;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceExtendedTest {

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

    private UUID sprintId;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        sprintId = UUID.randomUUID();
        sprint = TestFixtures.sprint(sprintId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner"))),
            SprintStatus.PLANNED);
    }

    @Test
    void start_activatesSprintWhenNoneActive() {
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStatus(sprint.getProject().getId(), SprintStatus.ACTIVE)).thenReturn(false);
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(SprintResponse.builder().id(sprintId).build());

        sprintService.start(sprintId);

        assert sprint.getStatus() == SprintStatus.ACTIVE;
        verify(readCacheEviction).evictBoard(sprint.getProject().getId());
    }

    @Test
    void delete_removesSprintIssuesFirst() {
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        sprintService.delete(sprintId);

        verify(sprintIssueRepository).deleteBySprintId(sprintId);
        verify(sprintRepository).delete(sprint);
    }

    @Test
    void removeIssue_deletesSprintIssue() {
        UUID issueId = UUID.randomUUID();
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        sprintService.removeIssue(sprintId, issueId);

        verify(sprintIssueRepository).deleteById(new SprintIssueId(sprintId, issueId));
    }

    @Test
    void reorderIssues_updatesKnownIssues() {
        UUID issueId = UUID.randomUUID();
        var issue = TestFixtures.issue(issueId, sprint.getProject());
        SprintIssue si = SprintIssue.builder()
            .id(new SprintIssueId(sprintId, issueId))
            .sprint(sprint)
            .issue(issue)
            .position(BigDecimal.ONE)
            .build();
        when(sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprintId)).thenReturn(List.of(si));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        sprintService.reorderIssues(sprintId, List.of(issueId));

        verify(sprintIssueRepository).save(si);
    }
}
