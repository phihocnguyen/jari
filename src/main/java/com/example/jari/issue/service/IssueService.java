package com.example.jari.issue.service;

import com.example.jari.issue.dto.*;
import com.example.jari.issue.entity.*;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.*;
import com.example.jari.issue.spec.IssueSpecification;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.shared.response.PageResponse;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;
    private final IssueHistoryService historyService;
    private final IssueMapper mapper;

    @Transactional
    public IssueResponse create(UUID projectId, UUID reporterId, CreateIssueRequest req) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        int nextNum = issueRepository.findMaxIssueNumber(projectId) + 1;
        String issueKey = project.getProjectKey() + "-" + nextNum;

        Issue issue = Issue.builder()
            .project(project)
            .issueKey(issueKey)
            .title(req.getTitle())
            .description(req.getDescription())
            .issueType(resolveIssueType(req.getIssueTypeId()))
            .status(resolveStatus(req.getStatusId()))
            .priority(resolvePriority(req.getPriorityId()))
            .reporter(reporter)
            .assignee(req.getAssigneeId() != null ? resolveUser(req.getAssigneeId()) : null)
            .parent(req.getParentId() != null ? resolveIssue(req.getParentId()) : null)
            .storyPoints(req.getStoryPoints())
            .dueDate(req.getDueDate())
            .build();

        return mapper.toResponse(issueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(UUID projectId, IssueFilterRequest filter) {
        var spec = IssueSpecification.filter(
            projectId,
            filter.getStatusId(),
            filter.getAssigneeId(),
            filter.getIssueTypeId(),
            filter.getPriorityId(),
            filter.getSprintId(),
            filter.getKeyword());

        var pageable = PageRequest.of(filter.getPage(), filter.getSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        return PageResponse.of(issueRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public IssueResponse get(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public IssueResponse update(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor  = resolveUser(actorId);

        if (req.getTitle()       != null) { historyService.record(issue, actor, "title",    issue.getTitle(),   req.getTitle());   issue.setTitle(req.getTitle()); }
        if (req.getDescription() != null) issue.setDescription(req.getDescription());
        if (req.getStatusId()    != null) {
            Status newStatus = resolveStatus(req.getStatusId());
            historyService.record(issue, actor, "status", issue.getStatus().getName(), newStatus.getName());
            issue.setStatus(newStatus);
        }
        if (req.getPriorityId()  != null) {
            Priority newPriority = resolvePriority(req.getPriorityId());
            historyService.record(issue, actor, "priority", issue.getPriority().getName(), newPriority.getName());
            issue.setPriority(newPriority);
        }
        if (req.getAssigneeId()  != null) {
            User newAssignee = resolveUser(req.getAssigneeId());
            historyService.record(issue, actor, "assignee",
                issue.getAssignee() != null ? issue.getAssignee().getDisplayName() : null,
                newAssignee.getDisplayName());
            issue.setAssignee(newAssignee);
        }
        if (req.getIssueTypeId() != null) issue.setIssueType(resolveIssueType(req.getIssueTypeId()));
        if (req.getParentId()    != null) issue.setParent(resolveIssue(req.getParentId()));
        if (req.getStoryPoints() != null) issue.setStoryPoints(req.getStoryPoints());
        if (req.getDueDate()     != null) issue.setDueDate(req.getDueDate());

        return mapper.toResponse(issueRepository.save(issue));
    }

    @Transactional
    public void delete(UUID id) {
        issueRepository.delete(findOrThrow(id));
    }

    private Issue    findOrThrow(UUID id)    { return issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue", id)); }
    private User     resolveUser(UUID id)    { return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    private IssueType resolveIssueType(UUID id) { return issueTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("IssueType", id)); }
    private Status    resolveStatus(UUID id)    { return statusRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Status", id)); }
    private Priority  resolvePriority(UUID id)  { return priorityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Priority", id)); }
    private Issue     resolveIssue(UUID id)     { return issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue", id)); }
}
