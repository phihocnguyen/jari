package com.example.jari.issue.service;

import com.example.jari.issue.dto.*;
import com.example.jari.issue.entity.*;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.*;
import com.example.jari.issue.spec.IssueSpecification;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.shared.response.PageResponse;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.example.jari.sprint.repository.SprintIssueRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;
    private final IssueHistoryRepository historyRepository;
    private final CommentRepository commentRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final com.example.jari.sprint.repository.SprintRepository sprintRepository;
    private final LabelRepository labelRepository;
    private final com.example.jari.release.repository.ReleaseRepository releaseRepository;
    private final com.example.jari.component.repository.ComponentRepository componentRepository;
    private final IssueHistoryService historyService;
    private final IssueMapper mapper;
    private final NotificationService notificationService;
    private final IssueWatcherRepository issueWatcherRepository;
    private final com.example.jari.automation.service.AutomationService automationService;
    private final com.example.jari.issue.search.IssueSearchService issueSearchService;
    private final ApplicationEventPublisher eventPublisher;

    private void notifyWatchersIssueUpdated(Issue issue, User actor) {
        try {
            List<IssueWatcher> watchers = issueWatcherRepository.findByIdIssueId(issue.getId());
            for (IssueWatcher w : watchers) {
                notificationService.notifyIssueUpdated(issue, actor, w.getUser());
            }
        } catch (Exception e) {
            log.warn("Failed to notify watchers for issue {}: {}", issue.getIssueKey(), e.getMessage());
        }
    }

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
            .startDate(req.getStartDate())
            .dueDate(req.getDueDate())
            .build();

        Issue saved = issueRepository.save(issue);

        if (saved.getAssignee() != null) {
            notificationService.notifyIssueAssigned(saved, reporter, saved.getAssignee());
        }
        notificationService.notifyIssueDueSoon(saved, saved.getAssignee());

        if (req.getSprintId() != null) {
            var sprint = sprintRepository.findById(req.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", req.getSprintId()));
            var si = com.example.jari.sprint.entity.SprintIssue.builder()
                .id(new com.example.jari.sprint.entity.SprintIssueId(sprint.getId(), saved.getId()))
                .sprint(sprint)
                .issue(saved)
                .position(BigDecimal.valueOf(1000))
                .build();
            sprintIssueRepository.save(si);
            saved.getSprintIssues().add(si);
        }

        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(UUID projectId, IssueFilterRequest filter) {
        // Đường chính: filter/search trên Elasticsearch (index jari-issues do Logstash indexer đồng bộ),
        // DB chỉ hydrate entity theo ID. ES lỗi/không khả dụng thì fallback về JPA như cũ.
        PageResponse<IssueResponse> searched = issueSearchService.search(projectId, filter);
        if (searched != null) {
            return searched;
        }

        var spec = IssueSpecification.filter(
            projectId,
            filter.getStatusId(),
            filter.getAssigneeId(),
            filter.getIssueTypeId(),
            filter.getPriorityId(),
            filter.getSprintId(),
            filter.getKeyword());

        var pageable = PageRequest.of(filter.getPage(), filter.getSize(),
            Sort.by(Sort.Direction.ASC, "position").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(issueRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public IssueResponse get(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public IssueResponse getByIdOrKey(String idOrKey) {
        Issue issue;
        try {
            issue = findOrThrow(UUID.fromString(idOrKey));
        } catch (IllegalArgumentException ex) {
            // Not a UUID — treat as issue key like "MOBILE-5"
            issue = issueRepository.findByIssueKeyIgnoreCase(idOrKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", idOrKey));
        }
        return mapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse update(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor  = resolveUser(actorId);

        if (req.getTitle()       != null) { historyService.record(issue, actor, "title",    issue.getTitle(),   req.getTitle());   issue.setTitle(req.getTitle()); }
        if (req.getDescription() != null) issue.setDescription(req.getDescription());
        if (req.getStatusId() != null || req.getStatus() != null) {
            Status newStatus = resolveStatusByNameOrId(req.getStatus(), req.getStatusId());
            if (issue.getStatus() == null || !issue.getStatus().getId().equals(newStatus.getId())) {
                historyService.record(issue, actor, "status", issue.getStatus() != null ? issue.getStatus().getName() : null, newStatus.getName());
                issue.setStatus(newStatus);
            }
        }
        if (req.getPriorityId() != null || req.getPriority() != null) {
            Priority newPriority = resolvePriorityByNameOrId(req.getPriority(), req.getPriorityId());
            if (issue.getPriority() == null || !issue.getPriority().getId().equals(newPriority.getId())) {
                historyService.record(issue, actor, "priority", issue.getPriority() != null ? issue.getPriority().getName() : null, newPriority.getName());
                issue.setPriority(newPriority);
            }
        }
        if (req.getAssigneeId()  != null) {
            User newAssignee = resolveUser(req.getAssigneeId());
            boolean changed = issue.getAssignee() == null || !issue.getAssignee().getId().equals(newAssignee.getId());
            historyService.record(issue, actor, "assignee",
                issue.getAssignee() != null ? issue.getAssignee().getDisplayName() : null,
                newAssignee.getDisplayName());
            issue.setAssignee(newAssignee);
            if (changed) notificationService.notifyIssueAssigned(issue, actor, newAssignee);
        }
        if (req.getIssueTypeId() != null) issue.setIssueType(resolveIssueType(req.getIssueTypeId()));
        if (req.getParentId()    != null) issue.setParent(resolveIssue(req.getParentId()));
        if (req.getStoryPoints() != null) issue.setStoryPoints(req.getStoryPoints());
        if (req.getStartDate()   != null) issue.setStartDate(req.getStartDate());
        if (req.getDueDate()     != null) issue.setDueDate(req.getDueDate());
        if (req.getSprintId() != null) {
            issue.getSprintIssues().clear();
            issueRepository.saveAndFlush(issue);
            var sprint = sprintRepository.findById(req.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", req.getSprintId()));
            var si = com.example.jari.sprint.entity.SprintIssue.builder()
                .id(new com.example.jari.sprint.entity.SprintIssueId(sprint.getId(), issue.getId()))
                .sprint(sprint)
                .issue(issue)
                .position(BigDecimal.valueOf(1000))
                .build();
            issue.getSprintIssues().add(si);
        }

        Issue saved = issueRepository.save(issue);
        automationService.onIssueStatusChanged(saved, actor);
        if (req.getDueDate() != null) {
            notificationService.notifyIssueDueSoon(issue, issue.getAssignee());
        }
        notifyWatchersIssueUpdated(saved, actor);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse updateStatus(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor  = resolveUser(actorId);

        Status newStatus = resolveStatusByNameOrId(req.getStatus(), req.getStatusId());
        if (issue.getStatus() == null || !issue.getStatus().getId().equals(newStatus.getId())) {
            historyService.record(issue, actor, "status",
                issue.getStatus() != null ? issue.getStatus().getName() : null,
                newStatus.getName());
            issue.setStatus(newStatus);
        }

        Issue saved = issueRepository.save(issue);
        automationService.onIssueStatusChanged(saved, actor);
        notifyWatchersIssueUpdated(saved, actor);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse updateAssignee(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor  = resolveUser(actorId);

        User newAssignee = req.getAssigneeId() != null ? resolveUser(req.getAssigneeId()) : null;
        boolean changed = !Objects.equals(
            issue.getAssignee() != null ? issue.getAssignee().getId() : null,
            newAssignee != null ? newAssignee.getId() : null);
        historyService.record(issue, actor, "assignee",
            issue.getAssignee() != null ? issue.getAssignee().getDisplayName() : null,
            newAssignee != null ? newAssignee.getDisplayName() : "Unassigned");
        issue.setAssignee(newAssignee);

        if (changed && newAssignee != null) {
            notificationService.notifyIssueAssigned(issue, actor, newAssignee);
        }

        Issue saved = issueRepository.save(issue);
        notifyWatchersIssueUpdated(saved, actor);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse updatePriority(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor  = resolveUser(actorId);

        Priority newPriority = resolvePriorityByNameOrId(req.getPriority(), req.getPriorityId());
        if (issue.getPriority() == null || !issue.getPriority().getId().equals(newPriority.getId())) {
            historyService.record(issue, actor, "priority",
                issue.getPriority() != null ? issue.getPriority().getName() : null,
                newPriority.getName());
            issue.setPriority(newPriority);
        }

        Issue saved = issueRepository.save(issue);
        notifyWatchersIssueUpdated(saved, actor);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse updateDates(UUID id, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        // Both fields are applied as given; null clears the date
        issue.setStartDate(req.getStartDate());
        issue.setDueDate(req.getDueDate());
        IssueResponse response = mapper.toResponse(issueRepository.save(issue));
        notificationService.notifyIssueDueSoon(issue, issue.getAssignee());
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(issue.getId()));
        return response;
    }

    @Transactional
    public IssueResponse updateParent(UUID id, UUID actorId, UpdateIssueRequest req) {
        Issue issue = findOrThrow(id);
        User actor = resolveUser(actorId);

        Issue newParent = req.getParentId() != null ? resolveIssue(req.getParentId()) : null;
        if (newParent != null) {
            if (newParent.getId().equals(issue.getId())) {
                throw new IllegalArgumentException("An issue cannot be its own parent");
            }
            // Walk up the ancestor chain to prevent cycles
            Issue ancestor = newParent;
            while (ancestor.getParent() != null) {
                if (ancestor.getParent().getId().equals(issue.getId())) {
                    throw new IllegalArgumentException("Cannot set a descendant as parent");
                }
                ancestor = ancestor.getParent();
            }
        }
        historyService.record(issue, actor, "parent",
            issue.getParent() != null ? issue.getParent().getIssueKey() : null,
            newParent != null ? newParent.getIssueKey() : null);
        issue.setParent(newParent);

        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse setLabels(UUID id, List<UUID> labelIds) {
        Issue issue = findOrThrow(id);
        issue.getLabels().clear();
        if (labelIds != null && !labelIds.isEmpty()) {
            issue.getLabels().addAll(labelRepository.findAllById(labelIds));
        }
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse setComponents(UUID id, List<UUID> componentIds) {
        Issue issue = findOrThrow(id);
        issue.getComponents().clear();
        if (componentIds != null && !componentIds.isEmpty()) {
            issue.getComponents().addAll(componentRepository.findAllById(componentIds));
        }
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse setRelease(UUID id, UUID releaseId) {
        Issue issue = findOrThrow(id);
        if (releaseId != null) {
            com.example.jari.release.entity.Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Release", releaseId));
            issue.setRelease(release);
        } else {
            issue.setRelease(null);
        }
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(saved.getId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IssueResponse updateSprint(UUID id, UUID actorId, UUID sprintId) {
        Issue issue = findOrThrow(id);
        User actor = actorId != null ? resolveUser(actorId) : null;

        UUID currentSprintId = (issue.getSprintIssues() == null || issue.getSprintIssues().isEmpty())
            ? null
            : issue.getSprintIssues().iterator().next().getSprint() != null
                ? issue.getSprintIssues().iterator().next().getSprint().getId()
                : null;

        if (java.util.Objects.equals(currentSprintId, sprintId)) {
            return mapper.toResponse(issue);
        }

        String oldSprintName = (issue.getSprintIssues() == null || issue.getSprintIssues().isEmpty())
            ? "Backlog"
            : issue.getSprintIssues().iterator().next().getSprint() != null
                ? issue.getSprintIssues().iterator().next().getSprint().getName()
                : "Backlog";

        // With cascade = ALL and orphanRemoval = true on issue.sprintIssues,
        // clearing the collection instructs Hibernate to delete the orphan record cleanly.
        // Calling sprintIssueRepository.deleteByIssueId() beforehand causes StaleObjectStateException
        // because Hibernate will attempt to delete rows already removed by the bulk query.
        issue.getSprintIssues().clear();
        issueRepository.saveAndFlush(issue);

        String newSprintName = "Backlog";
        if (sprintId != null) {
            com.example.jari.sprint.entity.Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));
            newSprintName = sprint.getName();

            List<com.example.jari.sprint.entity.SprintIssue> existing = sprintIssueRepository.findByIdSprintIdOrderByPositionAsc(sprint.getId());
            BigDecimal maxPos = existing.isEmpty() ? BigDecimal.valueOf(1000)
                : existing.get(existing.size() - 1).getPosition().add(BigDecimal.valueOf(1000));

            var si = com.example.jari.sprint.entity.SprintIssue.builder()
                .id(new com.example.jari.sprint.entity.SprintIssueId(sprint.getId(), issue.getId()))
                .sprint(sprint)
                .issue(issue)
                .position(maxPos)
                .build();
            issue.getSprintIssues().add(si);
            issueRepository.save(issue);
        }

        if (actor != null) {
            historyService.record(issue, actor, "sprint", oldSprintName, newSprintName);
        }

        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(issue.getId()));
        return mapper.toResponse(issue);
    }

    @Transactional
    public void delete(UUID id) {
        Issue issue = findOrThrow(id);
        issueRepository.detachParentFromChildIssues(id);
        commentRepository.deleteByIssueId(id);
        historyRepository.deleteByIssueId(id);
        issue.getSprintIssues().clear();
        issueRepository.delete(issue);
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.delete(id));
    }

    public Status resolveStatusByNameOrId(String statusStr, UUID statusId) {
        if (statusId != null) {
            return statusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", statusId));
        }
        if (statusStr != null && !statusStr.isBlank()) {
            String norm = statusStr.trim();
            Optional<Status> found = statusRepository.findByName(norm);
            if (found.isPresent()) return found.get();

            String altName = norm.replace("_", " ");
            found = statusRepository.findByName(altName);
            if (found.isPresent()) return found.get();

            found = statusRepository.findAll().stream()
                .filter(s -> s.getCategory() != null && s.getCategory().equalsIgnoreCase(norm))
                .findFirst();
            if (found.isPresent()) return found.get();

            // Default fallback to first status if not matched
            return statusRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Status", statusStr));
        }
        return statusRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Status", "default"));
    }

    public Priority resolvePriorityByNameOrId(String priorityStr, UUID priorityId) {
        if (priorityId != null) {
            return priorityRepository.findById(priorityId)
                .orElseThrow(() -> new ResourceNotFoundException("Priority", priorityId));
        }
        if (priorityStr != null && !priorityStr.isBlank()) {
            String norm = priorityStr.trim().toUpperCase();
            Optional<Priority> found = priorityRepository.findByName(norm);
            if (found.isPresent()) return found.get();

            return priorityRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Priority", priorityStr));
        }
        return priorityRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Priority", "default"));
    }

    @Transactional
    public void reorderIssues(UUID projectId, List<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) return;
        for (int i = 0; i < issueIds.size(); i++) {
            UUID issueId = issueIds.get(i);
            BigDecimal newPos = BigDecimal.valueOf((i + 1) * 1000L);
            issueRepository.updatePosition(issueId, newPos);
        }
        // updatePosition là bulk JPQL (bỏ qua Hibernate dirty checking) nên phải index lại thủ công
        eventPublisher.publishEvent(com.example.jari.issue.search.IssueIndexEvent.upsert(issueIds));
    }

    private Issue    findOrThrow(UUID id)    { return issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue", id)); }
    private User     resolveUser(UUID id)    { return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    private IssueType resolveIssueType(UUID id) { return issueTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("IssueType", id)); }
    private Status    resolveStatus(UUID id)    { return statusRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Status", id)); }
    private Priority  resolvePriority(UUID id)  { return priorityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Priority", id)); }
    private Issue     resolveIssue(UUID id)     { return issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue", id)); }
}
