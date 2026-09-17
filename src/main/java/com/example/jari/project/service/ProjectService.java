package com.example.jari.project.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.dto.*;
import com.example.jari.project.entity.*;
import com.example.jari.project.mapper.ProjectMapper;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;
    private final ProjectMapper mapper;
    private final NotificationService notificationService;

    @Transactional
    public ProjectResponse create(UUID workspaceId, UUID requesterId, CreateProjectRequest req) {
        Workspace ws = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        if (projectRepository.existsByWorkspaceIdAndProjectKey(workspaceId, req.getProjectKey())) {
            throw new ConflictException("Project key already exists in this workspace: " + req.getProjectKey());
        }
        User lead = req.getLeadId() != null
            ? userRepository.findById(req.getLeadId()).orElseThrow(() -> new ResourceNotFoundException("User", req.getLeadId()))
            : null;

        Project project = projectRepository.save(Project.builder()
            .workspace(ws)
            .name(req.getName())
            .projectKey(req.getProjectKey())
            .description(req.getDescription())
            .lead(lead)
            .projectType(req.getProjectType())
            .status(ProjectStatus.ACTIVE)
            .build());

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));
        memberRepository.save(ProjectMember.builder()
            .id(new ProjectMemberId(project.getId(), requesterId))
            .project(project)
            .user(requester)
            .role(rbacService.getRoleByName("PROJECT_ADMIN"))
            .build());

        return mapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByWorkspace(UUID workspaceId) {
        return projectRepository.findByWorkspaceId(workspaceId).stream()
            .map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest req) {
        Project p = findOrThrow(id);
        if (req.getName()        != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getStatus()      != null) p.setStatus(req.getStatus());
        if (req.getLeadId()      != null) {
            User lead = userRepository.findById(req.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getLeadId()));
            p.setLead(lead);
        }
        return mapper.toResponse(projectRepository.save(p));
    }

    @Transactional
    public void delete(UUID id) {
        projectRepository.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId) {
        return memberRepository.findByIdProjectId(projectId).stream()
            .map(mapper::toMemberResponse).toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, UUID actorId, AddProjectMemberRequest req) {
        Project project = findOrThrow(projectId);
        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, req.getUserId())) {
            throw new ConflictException("User is already a project member");
        }
        User user = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        ProjectMember member = memberRepository.save(ProjectMember.builder()
            .id(new ProjectMemberId(projectId, req.getUserId()))
            .project(project).user(user)
            .role(rbacService.getRoleByName(req.getRoleName()))
            .build());

        User actor = actorId != null
            ? userRepository.findById(actorId).orElse(null)
            : null;
        notificationService.notifyProjectMemberAdded(project, actor, user);

        return mapper.toMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        memberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    private Project findOrThrow(UUID id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
