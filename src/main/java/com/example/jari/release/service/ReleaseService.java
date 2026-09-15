package com.example.jari.release.service;

import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.release.dto.CreateReleaseRequest;
import com.example.jari.release.dto.ReleaseResponse;
import com.example.jari.release.entity.Release;
import com.example.jari.release.repository.ReleaseRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<ReleaseResponse> list(UUID projectId) {
        return releaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ReleaseResponse create(UUID projectId, CreateReleaseRequest req) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Release release = Release.builder()
            .project(project)
            .name(req.getName().trim())
            .description(req.getDescription())
            .releaseDate(req.getReleaseDate())
            .status("UNRELEASED")
            .build();
        return toResponse(releaseRepository.save(release));
    }

    private ReleaseResponse toResponse(Release release) {
        return ReleaseResponse.builder()
            .id(release.getId())
            .projectId(release.getProject().getId())
            .name(release.getName())
            .description(release.getDescription())
            .status(release.getStatus())
            .releaseDate(release.getReleaseDate())
            .createdAt(release.getCreatedAt())
            .build();
    }
}
