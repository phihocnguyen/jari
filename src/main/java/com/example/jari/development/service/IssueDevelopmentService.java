package com.example.jari.development.service;

import com.example.jari.development.dto.CreateDevelopmentRequest;
import com.example.jari.development.dto.IssueDevelopmentResponse;
import com.example.jari.development.entity.IssueDevelopment;
import com.example.jari.development.repository.IssueDevelopmentRepository;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueDevelopmentService {

    private final IssueDevelopmentRepository developmentRepository;
    private final IssueRepository issueRepository;

    @Transactional(readOnly = true)
    public List<IssueDevelopmentResponse> list(UUID issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }
        return developmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public IssueDevelopmentResponse create(UUID issueId, CreateDevelopmentRequest req) {
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        IssueDevelopment dev = IssueDevelopment.builder()
            .issue(issue)
            .type(req.getType().toUpperCase().trim())
            .repoUrl(req.getRepoUrl())
            .title(req.getTitle().trim())
            .url(req.getUrl().trim())
            .status(req.getStatus() != null ? req.getStatus().toUpperCase().trim() : "OPEN")
            .author(req.getAuthor())
            .build();

        return toResponse(developmentRepository.save(dev));
    }

    @Transactional
    public void delete(UUID id) {
        if (!developmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("IssueDevelopment", id);
        }
        developmentRepository.deleteById(id);
    }

    private IssueDevelopmentResponse toResponse(IssueDevelopment dev) {
        return IssueDevelopmentResponse.builder()
            .id(dev.getId())
            .issueId(dev.getIssue() != null ? dev.getIssue().getId() : null)
            .type(dev.getType())
            .repoUrl(dev.getRepoUrl())
            .title(dev.getTitle())
            .url(dev.getUrl())
            .status(dev.getStatus())
            .author(dev.getAuthor())
            .createdAt(dev.getCreatedAt())
            .build();
    }
}
