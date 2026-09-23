package com.example.jari.component.service;

import com.example.jari.component.dto.CreateComponentRequest;
import com.example.jari.component.dto.UpdateComponentRequest;
import com.example.jari.component.entity.Component;
import com.example.jari.component.repository.ComponentRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.dto.UserResponse;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
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
class ComponentServiceTest {

    @Mock private ComponentRepository componentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private ComponentService componentService;

    @Test
    void list_requiresExistingProject() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> componentService.list(projectId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_rejectsDuplicateName() {
        UUID projectId = UUID.randomUUID();
        CreateComponentRequest req = new CreateComponentRequest();
        req.setName("Backend");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(TestFixtures.project(projectId,
            TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")))));
        when(componentRepository.existsByProjectIdAndNameIgnoreCase(projectId, "Backend")).thenReturn(true);

        assertThatThrownBy(() -> componentService.create(projectId, req))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withLead() {
        UUID projectId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        var lead = TestFixtures.user(leadId, "Lead");
        var project = TestFixtures.project(projectId, TestFixtures.workspace(projectId, lead));
        CreateComponentRequest req = new CreateComponentRequest();
        req.setName(" API ");
        req.setLeadId(leadId);
        Component saved = Component.builder().id(UUID.randomUUID()).project(project).name("API").lead(lead).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(componentRepository.existsByProjectIdAndNameIgnoreCase(projectId, "API")).thenReturn(false);
        when(userRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(componentRepository.save(any())).thenReturn(saved);
        when(componentRepository.countIssuesByComponentId(saved.getId())).thenReturn(2L);
        when(userMapper.toResponse(lead)).thenReturn(UserResponse.builder().id(leadId).build());

        var response = componentService.create(projectId, req);

        assertThat(response.getName()).isEqualTo("API");
        assertThat(response.getIssueCount()).isEqualTo(2);
    }

    @Test
    void update_rejectsDuplicateRename() {
        UUID componentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        var project = TestFixtures.project(projectId, TestFixtures.workspace(projectId, TestFixtures.user(UUID.randomUUID(), "Owner")));
        Component component = Component.builder().id(componentId).project(project).name("Old").build();
        UpdateComponentRequest req = new UpdateComponentRequest();
        req.setName("Taken");

        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(componentRepository.existsByProjectIdAndNameIgnoreCase(projectId, "Taken")).thenReturn(true);

        assertThatThrownBy(() -> componentService.update(componentId, req))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_updatesDescriptionAndLead() {
        UUID componentId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        var lead = TestFixtures.user(leadId, "Lead");
        var project = TestFixtures.project(UUID.randomUUID(), TestFixtures.workspace(UUID.randomUUID(), lead));
        Component component = Component.builder().id(componentId).project(project).name("Core").build();
        UpdateComponentRequest req = new UpdateComponentRequest();
        req.setDescription("Updated");
        req.setLeadId(leadId);

        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(userRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(componentRepository.save(component)).thenReturn(component);
        when(componentRepository.countIssuesByComponentId(componentId)).thenReturn(0L);
        when(userMapper.toResponse(lead)).thenReturn(UserResponse.builder().id(leadId).build());

        componentService.update(componentId, req);

        assertThat(component.getDescription()).isEqualTo("Updated");
        assertThat(component.getLead()).isEqualTo(lead);
    }

    @Test
    void delete_requiresExistingComponent() {
        UUID id = UUID.randomUUID();
        when(componentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> componentService.delete(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_mapsComponents() {
        UUID projectId = UUID.randomUUID();
        Component component = Component.builder().id(UUID.randomUUID()).name("UI").build();
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(componentRepository.findByProjectIdOrderByNameAsc(projectId)).thenReturn(List.of(component));
        when(componentRepository.countIssuesByComponentId(component.getId())).thenReturn(1L);

        assertThat(componentService.list(projectId)).hasSize(1);
    }
}
