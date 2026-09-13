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

@Tag(name = "Projects")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project in workspace")
    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(projectService.create(workspaceId, user.getId(), req)));
    }

    @Operation(summary = "List projects in workspace")
    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.listByWorkspace(workspaceId)));
    }

    @Operation(summary = "Get project")
    @GetMapping("/api/v1/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.get(id)));
    }

    @Operation(summary = "Update project")
    @PutMapping("/api/v1/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.update(id, req)));
    }

    @Operation(summary = "Delete project")
    @DeleteMapping("/api/v1/projects/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted"));
    }

    @Operation(summary = "List project members")
    @GetMapping("/api/v1/projects/{id}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.listMembers(id)));
    }

    @Operation(summary = "Add member to project")
    @PostMapping("/api/v1/projects/{id}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddProjectMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(projectService.addMember(id, req)));
    }

    @Operation(summary = "Remove member from project")
    @DeleteMapping("/api/v1/projects/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id, @PathVariable UUID userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed"));
    }
}
