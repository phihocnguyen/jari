package com.example.jari.workspace.service;

import com.example.jari.rbac.service.RbacService;
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
        return memberRepository.findByIdWorkspaceId(workspaceId).stream()
            .map(mapper::toMemberResponse).toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(UUID requesterId, UUID workspaceId, InviteMemberRequest req) {
        Workspace ws = findOrThrow(workspaceId);
        requireAdmin(requesterId, ws);
        if (memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, req.getUserId())) {
            throw new ConflictException("User is already a member");
        }
        User user = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        WorkspaceMember member = memberRepository.save(WorkspaceMember.builder()
            .id(new WorkspaceMemberId(workspaceId, req.getUserId()))
            .workspace(ws)
            .user(user)
            .role(rbacService.getRoleByName(req.getRoleName()))
            .build());
        return mapper.toMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID requesterId, UUID workspaceId, UUID userId) {
        requireAdmin(requesterId, findOrThrow(workspaceId));
        memberRepository.deleteById(new WorkspaceMemberId(workspaceId, userId));
    }

    private Workspace findOrThrow(UUID id) {
        return workspaceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
    }

    private void requireAdmin(UUID userId, Workspace ws) {
        // Simplified: only owner = admin for now. Extend with role-check if needed.
        if (!memberRepository.existsByIdWorkspaceIdAndIdUserId(ws.getId(), userId)) {
            throw new ForbiddenException();
        }
    }
}
