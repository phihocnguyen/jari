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

@Tag(name = "Workspaces")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "Create workspace")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateWorkspaceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(workspaceService.create(user.getId(), req)));
    }

    @Operation(summary = "List my workspaces")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(workspaceService.listMyWorkspaces(user.getId())));
    }

    @Operation(summary = "Get workspace by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(workspaceService.get(id)));
    }

    @Operation(summary = "Update workspace")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(workspaceService.update(user.getId(), id, req)));
    }

    @Operation(summary = "Delete workspace")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id) {
        workspaceService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Workspace deleted"));
    }

    @Operation(summary = "List workspace members")
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(workspaceService.listMembers(id)));
    }

    @Operation(summary = "Invite member to workspace")
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> addMember(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(workspaceService.addMember(user.getId(), id, req)));
    }

    @Operation(summary = "Remove member from workspace")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        workspaceService.removeMember(user.getId(), id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed"));
    }
}
