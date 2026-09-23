package com.example.jari.release.service;

import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.release.dto.CreateReleaseRequest;
import com.example.jari.release.dto.UpdateReleaseRequest;
import com.example.jari.release.entity.Release;
import com.example.jari.release.repository.ReleaseRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    @Mock private ReleaseRepository releaseRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IssueRepository issueRepository;
    @InjectMocks private ReleaseService releaseService;

    @Test
    void create_setsUnreleasedStatus() {
        UUID projectId = UUID.randomUUID();
        var project = TestFixtures.project(projectId, TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")));
        CreateReleaseRequest req = new CreateReleaseRequest();
        req.setName(" v1.0 ");
        Release saved = Release.builder().id(UUID.randomUUID()).project(project).name("v1.0").status("UNRELEASED").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(releaseRepository.save(any())).thenReturn(saved);

        var response = releaseService.create(projectId, req);

        assertThat(response.getStatus()).isEqualTo("UNRELEASED");
        assertThat(response.getName()).isEqualTo("v1.0");
    }

    @Test
    void update_uppercasesStatus() {
        UUID releaseId = UUID.randomUUID();
        var project = TestFixtures.project(UUID.randomUUID(), TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner")));
        Release release = Release.builder().id(releaseId).project(project).name("v1").status("UNRELEASED").build();
        UpdateReleaseRequest req = new UpdateReleaseRequest();
        req.setStatus(" released ");
        req.setName(" v1.1 ");

        when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
        when(releaseRepository.save(release)).thenReturn(release);

        releaseService.update(releaseId, req);

        assertThat(release.getStatus()).isEqualTo("RELEASED");
        assertThat(release.getName()).isEqualTo("v1.1");
    }

    @Test
    void delete_detachesIssuesFirst() {
        UUID releaseId = UUID.randomUUID();
        var project = TestFixtures.project(UUID.randomUUID(), TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner")));
        Release release = Release.builder().id(releaseId).project(project).name("v1").build();
        when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));

        releaseService.delete(releaseId);

        verify(issueRepository).detachReleaseFromIssues(releaseId);
        verify(releaseRepository).delete(release);
    }

    @Test
    void update_ignoresBlankFields() {
        UUID releaseId = UUID.randomUUID();
        var project = TestFixtures.project(UUID.randomUUID(), TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Owner")));
        Release release = Release.builder().id(releaseId).project(project).name("v1").status("UNRELEASED").build();
        UpdateReleaseRequest req = new UpdateReleaseRequest();
        req.setName("   ");
        req.setStatus("  ");

        when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
        when(releaseRepository.save(release)).thenReturn(release);

        releaseService.update(releaseId, req);

        assertThat(release.getName()).isEqualTo("v1");
        assertThat(release.getStatus()).isEqualTo("UNRELEASED");
    }

    @Test
    void get_throwsWhenMissing() {
        UUID releaseId = UUID.randomUUID();
        when(releaseRepository.findById(releaseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> releaseService.get(releaseId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsProjectReleases() {
        UUID projectId = UUID.randomUUID();
        var project = TestFixtures.project(projectId, TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")));
        Release release = Release.builder().id(UUID.randomUUID()).project(project).name("v1").status("UNRELEASED").build();
        when(releaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(release));

        assertThat(releaseService.list(projectId)).hasSize(1);
    }
}
