package com.example.jari.project.controller;

import com.example.jari.project.dto.*;
import com.example.jari.project.service.ProjectService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
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

@Tag(name = "Projects", description = "Endpoints for managing projects and project members within a workspace")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project in workspace", description = "Creates a new project within a specific workspace. The user creating it becomes a project admin.")
    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<ProjectResponse>> create(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(projectService.create(workspaceId, user.getId(), req)));
    }

    @Operation(summary = "List projects in workspace", description = "Returns all projects belonging to a specific workspace.")
    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<ProjectResponse>>> list(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(projectService.listByWorkspace(workspaceId)));
    }

    @Operation(summary = "Get project", description = "Returns details of a specific project by its ID or project-key.")
    @GetMapping("/api/v1/projects/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<ProjectResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(projectService.get(id)));
    }

    @Operation(summary = "Update project", description = "Updates project details such as name, description, lead, and status.")
    @PutMapping("/api/v1/projects/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<ProjectResponse>> update(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(projectService.update(id, user.getId(), req)));
    }

    @Operation(summary = "Delete project", description = "Deletes a project permanently.")
    @DeleteMapping("/api/v1/projects/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        projectService.delete(id, user.getId());
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Project deleted"));
    }

    @Operation(summary = "List project members", description = "Returns all members assigned to a specific project.")
    @GetMapping("/api/v1/projects/{id}/members")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<ProjectMemberResponse>>> listMembers(@PathVariable String id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(projectService.listMembers(id)));
    }

    @Operation(summary = "Add member to project", description = "Adds a user to a project with a specific role.")
    @PostMapping("/api/v1/projects/{id}/members")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody AddProjectMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(projectService.addMember(id, user.getId(), req)));
    }

    @Operation(summary = "Update member role in project", description = "Updates a member's role in a project.")
    @PutMapping("/api/v1/projects/{id}/members/{userId}/role")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable String id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateProjectMemberRoleRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(
            projectService.updateMemberRole(id, user.getId(), userId, req.getRoleName())));
    }

    @Operation(summary = "Remove member from project", description = "Removes a user from a project.")
    @DeleteMapping("/api/v1/projects/{id}/members/{userId}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> removeMember(
            @PathVariable String id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails user) {
        projectService.removeMember(id, user.getId(), userId);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Member removed"));
    }
}
