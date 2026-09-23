package com.example.jari.issue.service;

import com.example.jari.issue.dto.CreateLabelRequest;
import com.example.jari.issue.entity.Label;
import com.example.jari.issue.repository.LabelRepository;
import com.example.jari.project.repository.ProjectRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock private LabelRepository labelRepository;
    @Mock private ProjectRepository projectRepository;
    @InjectMocks private LabelService labelService;

    @Test
    void create_returnsExistingLabelWhenNameMatches() {
        UUID projectId = UUID.randomUUID();
        Label existing = Label.builder().id(UUID.randomUUID()).name("bug").color("#fff").build();
        CreateLabelRequest req = new CreateLabelRequest();
        req.setName("BUG");
        when(labelRepository.findByProjectIdAndNameIgnoreCase(projectId, "BUG")).thenReturn(Optional.of(existing));

        var response = labelService.create(projectId, req);

        assertThat(response.getName()).isEqualTo("bug");
        verify(labelRepository, never()).save(any());
    }

    @Test
    void create_assignsDefaultColorWhenMissing() {
        UUID projectId = UUID.randomUUID();
        CreateLabelRequest req = new CreateLabelRequest();
        req.setName("feature");
        when(labelRepository.findByProjectIdAndNameIgnoreCase(projectId, "feature")).thenReturn(Optional.empty());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(TestFixtures.project(projectId,
            TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")))));
        when(labelRepository.findByProjectIdOrderByNameAsc(projectId)).thenReturn(List.of());
        when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = labelService.create(projectId, req);

        assertThat(response.getColor()).isNotBlank();
    }
}
