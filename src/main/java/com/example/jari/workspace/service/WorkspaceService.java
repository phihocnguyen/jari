package com.example.jari.workspace.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.dto.*;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMember;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import com.example.jari.workspace.mapper.WorkspaceMapper;
import com.example.jari.workspace.repository.WorkspaceMemberRepository;
import com.example.jari.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;
    private final WorkspaceMapper mapper;
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public WorkspaceResponse create(UUID ownerId, CreateWorkspaceRequest req) {
        if (workspaceRepository.existsByWorkspaceKey(req.getWorkspaceKey())) {
            throw new ConflictException("Workspace key already exists: " + req.getWorkspaceKey());
        }
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Workspace ws = workspaceRepository.save(Workspace.builder()
            .name(req.getName())
            .workspaceKey(req.getWorkspaceKey())
            .description(req.getDescription())
            .owner(owner)
            .build());

        // Add owner as WORKSPACE_ADMIN
        memberRepository.save(WorkspaceMember.builder()
            .id(new WorkspaceMemberId(ws.getId(), ownerId))
            .workspace(ws)
            .user(owner)
            .role(rbacService.getRoleByName("WORKSPACE_ADMIN"))
            .build());

        return mapper.toResponse(ws);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMyWorkspaces(UUID userId) {
        return workspaceRepository.findAllByMemberUserId(userId).stream()
            .map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public WorkspaceResponse update(UUID requesterId, UUID workspaceId, UpdateWorkspaceRequest req) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);
        if (req.getName() != null) ws.setName(req.getName());
        if (req.getDescription() != null) ws.setDescription(req.getDescription());
        return mapper.toResponse(workspaceRepository.save(ws));
    }

    @Transactional
    public void delete(UUID requesterId, UUID workspaceId) {
        Workspace ws = findOrThrow(workspaceId);
        if (!ws.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Only workspace owner can delete");
        }
        workspaceRepository.delete(ws);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(UUID workspaceId) {
        List<WorkspaceMember> members = memberRepository.findByIdWorkspaceId(workspaceId);
        return members.stream().map(m -> {
            WorkspaceMemberResponse resp = mapper.toMemberResponse(m);
            List<ProjectMember> pms = projectMemberRepository.findAllByUserIdAndWorkspaceId(m.getUser().getId(), workspaceId);
            resp.setProjectIds(pms.stream().map(pm -> pm.getProject().getId()).toList());
            resp.setProjectNames(pms.stream().map(pm -> pm.getProject().getName()).toList());
            return resp;
        }).toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(UUID requesterId, UUID workspaceId, InviteMemberRequest req) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);

        User targetUser;
        if (req.getUserId() != null) {
            targetUser = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        } else if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            targetUser = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + req.getEmail() + " not found. Please make sure they have registered."));
        } else {
            throw new BadRequestException("Either userId or email is required to invite a member");
        }

        if (memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, targetUser.getId())) {
            throw new ConflictException("User is already a member of this workspace");
        }

        String roleName = (req.getRoleName() != null && !req.getRoleName().trim().isEmpty())
            ? req.getRoleName().trim()
            : "WORKSPACE_MEMBER";

        WorkspaceMember member = memberRepository.save(WorkspaceMember.builder()
            .id(new WorkspaceMemberId(workspaceId, targetUser.getId()))
            .workspace(ws)
            .user(targetUser)
            .role(rbacService.getRoleByName(roleName))
            .build());

        // Assign to specified projects if any
        List<UUID> assignedProjectIds = new ArrayList<>();
        List<String> assignedProjectNames = new ArrayList<>();
        if (req.getProjectIds() != null && !req.getProjectIds().isEmpty()) {
            String projRole = "WORKSPACE_ADMIN".equals(roleName) ? "PROJECT_ADMIN" : "PROJECT_MEMBER";
            for (UUID pid : req.getProjectIds()) {
                Project p = projectRepository.findById(pid).orElse(null);
                if (p != null && p.getWorkspace().getId().equals(workspaceId)) {
                    if (!projectMemberRepository.existsByIdProjectIdAndIdUserId(pid, targetUser.getId())) {
                        projectMemberRepository.save(ProjectMember.builder()
                            .id(new ProjectMemberId(pid, targetUser.getId()))
                            .project(p)
                            .user(targetUser)
                            .role(rbacService.getRoleByName(projRole))
                            .build());
                    }
                    assignedProjectIds.add(p.getId());
                    assignedProjectNames.add(p.getName());
                }
            }
        }

        notificationService.notifyWorkspaceMemberAdded(ws, requesterId != null
            ? userRepository.findById(requesterId).orElse(null)
            : null, targetUser);

        WorkspaceMemberResponse resp = mapper.toMemberResponse(member);
        resp.setProjectIds(assignedProjectIds);
        resp.setProjectNames(assignedProjectNames);
        return resp;
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(UUID requesterId, UUID workspaceId, UUID targetUserId, UpdateMemberRoleRequest req) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);

        if (ws.getOwner().getId().equals(targetUserId)) {
            throw new ForbiddenException("Cannot change role of the workspace owner");
        }

        WorkspaceMember member = memberRepository.findById(new WorkspaceMemberId(workspaceId, targetUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        member.setRole(rbacService.getRoleByName(req.getRoleName()));
        memberRepository.save(member);

        WorkspaceMemberResponse resp = mapper.toMemberResponse(member);
        List<ProjectMember> pms = projectMemberRepository.findAllByUserIdAndWorkspaceId(targetUserId, workspaceId);
        resp.setProjectIds(pms.stream().map(pm -> pm.getProject().getId()).toList());
        resp.setProjectNames(pms.stream().map(pm -> pm.getProject().getName()).toList());
        return resp;
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberProjects(UUID requesterId, UUID workspaceId, UUID targetUserId, UpdateMemberProjectsRequest req) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);

        WorkspaceMember member = memberRepository.findById(new WorkspaceMemberId(workspaceId, targetUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        List<UUID> newProjectIds = req.getProjectIds() != null ? req.getProjectIds() : List.of();

        // 1. Remove projects that are no longer assigned
        List<ProjectMember> currentPms = projectMemberRepository.findAllByUserIdAndWorkspaceId(targetUserId, workspaceId);
        for (ProjectMember pm : currentPms) {
            if (!newProjectIds.contains(pm.getProject().getId())) {
                projectMemberRepository.delete(pm);
            }
        }

        // 2. Add projects in newProjectIds
        List<UUID> assignedIds = new ArrayList<>();
        List<String> assignedNames = new ArrayList<>();
        String projRole = "WORKSPACE_ADMIN".equals(member.getRole().getName()) ? "PROJECT_ADMIN" : "PROJECT_MEMBER";

        for (UUID pid : newProjectIds) {
            Project p = projectRepository.findById(pid).orElse(null);
            if (p != null && p.getWorkspace().getId().equals(workspaceId)) {
                if (!projectMemberRepository.existsByIdProjectIdAndIdUserId(pid, targetUserId)) {
                    projectMemberRepository.save(ProjectMember.builder()
                        .id(new ProjectMemberId(pid, targetUserId))
                        .project(p)
                        .user(member.getUser())
                        .role(rbacService.getRoleByName(projRole))
                        .build());
                }
                assignedIds.add(p.getId());
                assignedNames.add(p.getName());
            }
        }

        WorkspaceMemberResponse resp = mapper.toMemberResponse(member);
        resp.setProjectIds(assignedIds);
        resp.setProjectNames(assignedNames);
        return resp;
    }

    @Transactional
    public void removeMember(UUID requesterId, UUID workspaceId, UUID targetUserId) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);

        if (ws.getOwner().getId().equals(targetUserId)) {
            throw new ForbiddenException("Cannot remove the workspace owner");
        }

        memberRepository.deleteById(new WorkspaceMemberId(workspaceId, targetUserId));
        // Also remove member from all projects in this workspace
        projectMemberRepository.deleteAllByUserIdAndWorkspaceId(targetUserId, workspaceId);
    }

    private Workspace findOrThrow(UUID id) {
        return workspaceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
    }

    private void requireAdmin(UUID userId, Workspace ws) {
        if (userId == null) {
            throw new ForbiddenException("Authentication required");
        }
        boolean isOwner = ws.getOwner().getId().equals(userId);
        boolean isAdmin = memberRepository.findById(new WorkspaceMemberId(ws.getId(), userId))
            .map(m -> "WORKSPACE_ADMIN".equals(m.getRole().getName()))
            .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only workspace owner or workspace admins can perform this action");
        }
    }
}
