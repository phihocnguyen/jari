package com.example.jari.sprint.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.cache.CacheNames;
import com.example.jari.shared.cache.ReadCacheEviction;
import com.example.jari.shared.exception.ApiException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
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
    private final ReadCacheEviction readCacheEviction;

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

    @Transactional(readOnly = true)
    public SprintResponse get(UUID sprintId) {
        return sprintMapper.toResponse(findOrThrow(sprintId));
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
    public void delete(UUID sprintId) {
        Sprint sprint = findOrThrow(sprintId);
        sprintIssueRepository.deleteBySprintId(sprintId);
        sprintRepository.delete(sprint);
    }

    @Transactional
    public SprintResponse start(UUID sprintId) {
        Sprint sprint = findOrThrow(sprintId);
        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProject().getId(), SprintStatus.ACTIVE)) {
            throw new ConflictException("A sprint is already active in this project");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint saved = sprintRepository.save(sprint);
        readCacheEviction.evictBoard(saved.getProject().getId());
        return sprintMapper.toResponse(saved);
    }

    @Transactional
    public SprintResponse complete(UUID sprintId) {
        Sprint sprint = findOrThrow(sprintId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE", "Only active sprints can be completed");
        }
        sprint.setStatus(SprintStatus.COMPLETED);
        Sprint saved = sprintRepository.save(sprint);
        readCacheEviction.evictBoard(saved.getProject().getId());
        return sprintMapper.toResponse(saved);
    }

    @Transactional
    public void addIssue(UUID sprintId, AddIssueToSprintRequest req) {
        Sprint sprint = findOrThrow(sprintId);
        Issue issue   = issueRepository.findById(req.getIssueId())
            .orElseThrow(() -> new ResourceNotFoundException("Issue", req.getIssueId()));

        sprintIssueRepository.deleteByIssueId(req.getIssueId());
        SprintIssueId id = new SprintIssueId(sprintId, req.getIssueId());

        List<SprintIssue> existing = sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprintId);
        BigDecimal maxPos = existing.isEmpty() ? BigDecimal.ZERO
            : existing.get(existing.size() - 1).getPosition().add(BigDecimal.valueOf(1000));

        sprintIssueRepository.save(SprintIssue.builder()
            .id(id).sprint(sprint).issue(issue).position(maxPos).build());
        readCacheEviction.evictBoard(sprint.getProject().getId());
    }

    @Transactional
    public void removeIssue(UUID sprintId, UUID issueId) {
        Sprint sprint = findOrThrow(sprintId);
        sprintIssueRepository.deleteById(new SprintIssueId(sprintId, issueId));
        readCacheEviction.evictBoard(sprint.getProject().getId());
    }

    @Cacheable(value = CacheNames.PROJECT_BOARD, key = "#projectId")
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

    @Transactional
    public void reorderIssues(UUID sprintId, List<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) return;
        List<SprintIssue> existing = sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprintId);
        java.util.Map<UUID, SprintIssue> map = existing.stream()
            .collect(Collectors.toMap(si -> si.getIssue().getId(), si -> si));

        for (int i = 0; i < issueIds.size(); i++) {
            UUID issueId = issueIds.get(i);
            SprintIssue si = map.get(issueId);
            if (si != null) {
                si.setPosition(BigDecimal.valueOf((i + 1) * 1000L));
                sprintIssueRepository.save(si);
            }
        }
        readCacheEviction.evictBoard(findOrThrow(sprintId).getProject().getId());
    }

    @Transactional
    public void updateIssuePosition(UUID sprintId, UUID issueId, BigDecimal position) {
        Sprint sprint = findOrThrow(sprintId);
        SprintIssueId id = new SprintIssueId(sprintId, issueId);
        sprintIssueRepository.findById(id).ifPresent(si -> {
            si.setPosition(position);
            sprintIssueRepository.save(si);
        });
        readCacheEviction.evictBoard(sprint.getProject().getId());
    }

    private Sprint findOrThrow(UUID id) {
        return sprintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
    }
}
