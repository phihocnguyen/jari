package com.example.jari.component.service;

import com.example.jari.component.dto.ComponentResponse;
import com.example.jari.component.dto.CreateComponentRequest;
import com.example.jari.component.dto.UpdateComponentRequest;
import com.example.jari.component.entity.Component;
import com.example.jari.component.repository.ComponentRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<ComponentResponse> list(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        return componentRepository.findByProjectIdOrderByNameAsc(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ComponentResponse get(UUID id) {
        Component component = componentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Component", id));
        return toResponse(component);
    }

    @Transactional
    public ComponentResponse create(UUID projectId, CreateComponentRequest req) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (componentRepository.existsByProjectIdAndNameIgnoreCase(projectId, req.getName().trim())) {
            throw new BadRequestException("Component with name '" + req.getName().trim() + "' already exists in this project");
        }

        User lead = null;
        if (req.getLeadId() != null) {
            lead = userRepository.findById(req.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getLeadId()));
        }

        Component component = Component.builder()
            .project(project)
            .name(req.getName().trim())
            .description(req.getDescription())
            .lead(lead)
            .build();

        return toResponse(componentRepository.save(component));
    }

    @Transactional
    public ComponentResponse update(UUID id, UpdateComponentRequest req) {
        Component component = componentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Component", id));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(component.getName()) &&
                componentRepository.existsByProjectIdAndNameIgnoreCase(component.getProject().getId(), newName)) {
                throw new BadRequestException("Component with name '" + newName + "' already exists in this project");
            }
            component.setName(newName);
        }

        if (req.getDescription() != null) {
            component.setDescription(req.getDescription());
        }

        if (req.getLeadId() != null) {
            User lead = userRepository.findById(req.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getLeadId()));
            component.setLead(lead);
        }

        return toResponse(componentRepository.save(component));
    }

    @Transactional
    public void delete(UUID id) {
        if (!componentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Component", id);
        }
        componentRepository.deleteById(id);
    }

    public ComponentResponse toResponse(Component c) {
        long count = componentRepository.countIssuesByComponentId(c.getId());
        return ComponentResponse.builder()
            .id(c.getId())
            .projectId(c.getProject() != null ? c.getProject().getId() : null)
            .name(c.getName())
            .description(c.getDescription())
            .lead(c.getLead() != null ? userMapper.toResponse(c.getLead()) : null)
            .issueCount(count)
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }
}
