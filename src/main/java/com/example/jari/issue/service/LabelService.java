package com.example.jari.issue.service;

import com.example.jari.issue.dto.CreateLabelRequest;
import com.example.jari.issue.dto.LabelResponse;
import com.example.jari.issue.entity.Label;
import com.example.jari.issue.repository.LabelRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabelService {

    private static final String[] DEFAULT_COLORS = {
        "#0c66e4", "#22a06b", "#ca3521", "#6b21a8", "#d97706",
        "#0b8fa8", "#c9372c", "#5e6c84", "#ae4787", "#1f845a"
    };

    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<LabelResponse> list(UUID projectId) {
        return labelRepository.findByProjectIdOrderByNameAsc(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public LabelResponse create(UUID projectId, CreateLabelRequest req) {
        // Idempotent: typing an existing label name returns it instead of duplicating
        String name = req.getName().trim();
        var existing = labelRepository.findByProjectIdAndNameIgnoreCase(projectId, name);
        if (existing.isPresent()) return toResponse(existing.get());

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        long count = labelRepository.findByProjectIdOrderByNameAsc(projectId).size();
        Label label = Label.builder()
            .project(project)
            .name(name)
            .color(req.getColor() != null ? req.getColor() : DEFAULT_COLORS[(int) (count % DEFAULT_COLORS.length)])
            .build();
        return toResponse(labelRepository.save(label));
    }

    private LabelResponse toResponse(Label label) {
        return LabelResponse.builder()
            .id(label.getId())
            .name(label.getName())
            .color(label.getColor())
            .build();
    }
}
