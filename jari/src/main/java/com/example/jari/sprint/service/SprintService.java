package com.example.jari.sprint.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.ApiException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.sprint.dto.*;
import com.example.jari.sprint.entity.*;
import com.example.jari.sprint.mapper.SprintMapper;
import com.example.jari.sprint.repository.SprintIssueRepository;
import com.example.jari.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final StatusRepository statusRepository;
    private final SprintMapper sprintMapper;
    private final IssueMapper issueMapper;

    @Transactional
    public SprintResponse create(UUID projectId, CreateSprintRequest req) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        Sprint sprint = sprintRepository.save(Sprint.builder()
            .project(project).name(req.getName()).goal(req.getGoal())
            .startDate(req.getStartDate()).endDate(req.getEndDate())
            .status(SprintStatus.PLANNED).build());
        return sprintMapper.toResponse(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> list(UUID projectId) {
        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
            .map(sprintMapper::toResponse).toList();
    }

    @Transactional
    public SprintResponse update(UUID sprintId, UpdateSprintRequest req) {
        Sprint sprint = findOrThrow(sprintId);
        if (req.getName()      != null) sprint.setName(req.getName());
        if (req.getGoal()      != null) sprint.setGoal(req.getGoal());
        if (req.getStartDate() != null) sprint.setStartDate(req.getStartDate());
        if (req.getEndDate()   != null) sprint.setEndDate(req.getEndDate());
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintResponse start(UUID sprintId) {
        Sprint sprint = findOrThrow(sprintId);
        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProject().getId(), SprintStatus.ACTIVE)) {
            throw new ConflictException("A sprint is already active in this project");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintResponse complete(UUID sprintId) {
        Sprint sprint = findOrThrow(sprintId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE", "Only active sprints can be completed");
        }
        sprint.setStatus(SprintStatus.COMPLETED);
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public void addIssue(UUID sprintId, AddIssueToSprintRequest req) {
        Sprint sprint = findOrThrow(sprintId);
        Issue issue   = issueRepository.findById(req.getIssueId())
            .orElseThrow(() -> new ResourceNotFoundException("Issue", req.getIssueId()));

        SprintIssueId id = new SprintIssueId(sprintId, req.getIssueId());
        if (sprintIssueRepository.existsById(id)) throw new ConflictException("Issue already in sprint");

        List<SprintIssue> existing = sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprintId);
        BigDecimal maxPos = existing.isEmpty() ? BigDecimal.ZERO
            : existing.get(existing.size() - 1).getPosition().add(BigDecimal.valueOf(1000));

        sprintIssueRepository.save(SprintIssue.builder()
            .id(id).sprint(sprint).issue(issue).position(maxPos).build());
    }

    @Transactional
    public void removeIssue(UUID sprintId, UUID issueId) {
        sprintIssueRepository.deleteById(new SprintIssueId(sprintId, issueId));
    }

    @Transactional(readOnly = true)
    public List<BoardColumnResponse> getBoard(UUID projectId) {
        Sprint activeSprint = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Active Sprint", projectId));

        List<SprintIssue> sprintIssues = sprintIssueRepository
            .findByIdSprintIdOrderByPositionAsc(activeSprint.getId());

        var statuses = statusRepository.findAll();

        return statuses.stream().map(status -> {
            List<com.example.jari.issue.dto.IssueResponse> issues = sprintIssues.stream()
                .filter(si -> si.getIssue().getStatus().getId().equals(status.getId()))
                .map(si -> issueMapper.toResponse(si.getIssue()))
                .collect(Collectors.toList());
            return BoardColumnResponse.builder()
                .statusId(status.getId())
                .statusName(status.getName())
                .statusCategory(status.getCategory())
                .issues(issues)
                .build();
        }).collect(Collectors.toList());
    }

    private Sprint findOrThrow(UUID id) {
        return sprintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
    }
}
