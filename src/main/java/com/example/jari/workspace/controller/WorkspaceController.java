package com.example.jari.workspace.controller;

import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import com.example.jari.workspace.dto.*;
import com.example.jari.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Workspaces", description = "Endpoints for managing workspaces and their members")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "Create workspace", description = "Creates a new workspace with the authenticated user as the owner and admin.")
    @PostMapping
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceResponse>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateWorkspaceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(workspaceService.create(user.getId(), req)));
    }

    @Operation(summary = "List my workspaces", description = "Returns a list of workspaces the authenticated user is a member of.")
    @GetMapping
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<WorkspaceResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) UUID userId) {
        UUID effectiveUserId = (userId != null) ? userId : (user != null ? user.getId() : null);
        if (effectiveUserId == null) {
            return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(List.of()));
        }
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(workspaceService.listMyWorkspaces(effectiveUserId)));
    }

    @Operation(summary = "Get workspace by ID", description = "Returns details of a specific workspace.")
    @GetMapping("/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(workspaceService.get(id)));
    }

    @Operation(summary = "Update workspace", description = "Updates a workspace's name and description. Requires workspace admin privileges.")
    @PutMapping("/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceResponse>> update(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(workspaceService.update(user.getId(), id, req)));
    }

    @Operation(summary = "Delete workspace", description = "Deletes a workspace. Only the workspace owner can perform this action.")
    @DeleteMapping("/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id) {
        workspaceService.delete(user.getId(), id);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Workspace deleted"));
    }

    @Operation(summary = "List workspace members", description = "Returns all members of a workspace.")
    @GetMapping("/{id}/members")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<WorkspaceMemberResponse>>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(workspaceService.listMembers(id)));
    }

    @Operation(summary = "Invite member to workspace", description = "Adds a user to the workspace with a specific role. Requires workspace admin privileges.")
    @PostMapping("/{id}/members")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceMemberResponse>> addMember(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(workspaceService.addMember(user.getId(), id, req)));
    }

    @Operation(summary = "Update member role in workspace", description = "Updates a member's role in the workspace. Requires workspace admin privileges.")
    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceMemberResponse>> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(
            workspaceService.updateMemberRole(user.getId(), id, userId, req)));
    }

    @Operation(summary = "Update member project access in workspace", description = "Assigns or revokes project access for a workspace member. Requires workspace admin privileges.")
    @PutMapping("/{id}/members/{userId}/projects")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<WorkspaceMemberResponse>> updateMemberProjects(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberProjectsRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(
            workspaceService.updateMemberProjects(user.getId(), id, userId, req)));
    }

    @Operation(summary = "Remove member from workspace", description = "Removes a user from the workspace. Requires workspace admin privileges.")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        workspaceService.removeMember(user.getId(), id, userId);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Member removed"));
    }
}
