package com.example.jari.project.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.dto.*;
import com.example.jari.project.entity.*;
import com.example.jari.project.mapper.ProjectMapper;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import com.example.jari.workspace.repository.WorkspaceMemberRepository;
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
    private final WorkspaceMemberRepository workspaceMemberRepository;
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
            .avatarIcon(req.getAvatarIcon())
            .avatarColor(req.getAvatarColor())
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

    @Transactional(readOnly = true)
    public ProjectResponse get(String idOrKey) {
        return mapper.toResponse(findOrThrow(idOrKey));
    }

    @Transactional
    public ProjectResponse update(UUID id, UUID actorId, UpdateProjectRequest req) {
        return update(id.toString(), actorId, req);
    }

    @Transactional
    public ProjectResponse update(String idOrKey, UUID actorId, UpdateProjectRequest req) {
        Project p = findOrThrow(idOrKey);
        requireAdmin(p, actorId);
        if (req.getName()        != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getStatus()      != null) p.setStatus(req.getStatus());
        if (req.getAvatarIcon()  != null) p.setAvatarIcon(req.getAvatarIcon());
        if (req.getAvatarColor() != null) p.setAvatarColor(req.getAvatarColor());
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

    @Transactional
    public void delete(String idOrKey, UUID actorId) {
        Project p = findOrThrow(idOrKey);
        requireAdmin(p, actorId);
        projectRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId) {
        return memberRepository.findByIdProjectId(projectId).stream()
            .map(mapper::toMemberResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(String idOrKey) {
        Project project = findOrThrow(idOrKey);
        return memberRepository.findByIdProjectId(project.getId()).stream()
            .map(mapper::toMemberResponse).toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, UUID actorId, AddProjectMemberRequest req) {
        return addMember(projectId.toString(), actorId, req);
    }

    @Transactional
    public ProjectMemberResponse addMember(String idOrKey, UUID actorId, AddProjectMemberRequest req) {
        Project project = findOrThrow(idOrKey);
        requireAdmin(project, actorId);

        User user = null;
        if (req.getUserId() != null) {
            user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user = userRepository.findByEmail(req.getEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + req.getEmail()));
        } else {
            throw new BadRequestException("Either userId or email is required");
        }

        if (memberRepository.existsByIdProjectIdAndIdUserId(project.getId(), user.getId())) {
            throw new ConflictException("User is already a project member");
        }

        String role = req.getRoleName() != null ? req.getRoleName() : "PROJECT_MEMBER";
        ProjectMember member = memberRepository.save(ProjectMember.builder()
            .id(new ProjectMemberId(project.getId(), user.getId()))
            .project(project).user(user)
            .role(rbacService.getRoleByName(role))
            .build());

        User actor = actorId != null
            ? userRepository.findById(actorId).orElse(null)
            : null;
        notificationService.notifyProjectMemberAdded(project, actor, user);

        return mapper.toMemberResponse(member);
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(String idOrKey, UUID actorId, UUID userId, String roleName) {
        Project project = findOrThrow(idOrKey);
        requireAdmin(project, actorId);

        ProjectMember member = memberRepository.findById(new ProjectMemberId(project.getId(), userId))
            .orElseThrow(() -> new ResourceNotFoundException("Project member not found for user: " + userId));

        member.setRole(rbacService.getRoleByName(roleName));
        return mapper.toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        memberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    @Transactional
    public void removeMember(String idOrKey, UUID actorId, UUID userId) {
        Project project = findOrThrow(idOrKey);
        requireAdmin(project, actorId);

        if (project.getLead() != null && project.getLead().getId().equals(userId)) {
            throw new BadRequestException("Cannot remove the space owner/lead");
        }

        memberRepository.deleteById(new ProjectMemberId(project.getId(), userId));
    }

    public Project findOrThrow(UUID id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public Project findOrThrow(String idOrKey) {
        try {
            UUID uuid = UUID.fromString(idOrKey);
            return projectRepository.findById(uuid)
                .orElseGet(() -> projectRepository.findByProjectKeyIgnoreCase(idOrKey)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", idOrKey)));
        } catch (IllegalArgumentException e) {
            return projectRepository.findByProjectKeyIgnoreCase(idOrKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project", idOrKey));
        }
    }

    private void requireAdmin(Project project, UUID actorId) {
        if (actorId == null) return;
        // Project lead is admin
        if (project.getLead() != null && actorId.equals(project.getLead().getId())) {
            return;
        }
        // Project admin role
        boolean isProjectAdmin = memberRepository.findById(new ProjectMemberId(project.getId(), actorId))
            .map(pm -> "PROJECT_ADMIN".equalsIgnoreCase(pm.getRole().getName()))
            .orElse(false);
        if (isProjectAdmin) return;

        // Workspace owner
        if (project.getWorkspace().getOwner() != null && actorId.equals(project.getWorkspace().getOwner().getId())) {
            return;
        }

        // Workspace admin
        boolean isWsAdmin = workspaceMemberRepository.findById(new WorkspaceMemberId(project.getWorkspace().getId(), actorId))
            .map(wm -> "WORKSPACE_ADMIN".equalsIgnoreCase(wm.getRole().getName()))
            .orElse(false);
        if (isWsAdmin) return;

        throw new ForbiddenException("Only space admins or workspace admins can perform this action");
    }
}
