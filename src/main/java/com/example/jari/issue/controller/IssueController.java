package com.example.jari.issue.controller;

import com.example.jari.issue.dto.*;
import com.example.jari.issue.service.IssueHistoryService;
import com.example.jari.issue.service.IssueService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.response.PageResponse;
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

@Tag(name = "Issues", description = "Endpoints for managing issues, filtering, and retrieving history")
@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final IssueHistoryService historyService;

    @Operation(summary = "Create issue", description = "Creates a new issue in a specific project. Generates a unique issue key automatically.")
    @PostMapping("/api/v1/projects/{projectId}/issues")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> create(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateIssueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(issueService.create(projectId, user.getId(), req)));
    }

    @Operation(summary = "List issues with filter", description = "Returns a paginated list of issues for a project, with optional dynamic filtering (status, assignee, sprint, etc.).")
    @GetMapping("/api/v1/projects/{projectId}/issues")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<com.example.jari.shared.response.PageResponse<IssueResponse>>> list(
            @PathVariable UUID projectId,
            @ModelAttribute IssueFilterRequest filter) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.list(projectId, filter)));
    }

    @Operation(summary = "Get issue", description = "Returns details of a specific issue.")
    @GetMapping("/api/v1/issues/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.get(id)));
    }

    @Operation(summary = "Update issue", description = "Updates an issue's fields. Generates audit history records for changed fields automatically.")
    @PutMapping("/api/v1/issues/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.update(id, user.getId(), req)));
    }

    @Operation(summary = "Update issue status", description = "Updates an issue's status.")
    @PatchMapping("/api/v1/issues/{id}/status")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> updateStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.updateStatus(id, user.getId(), req)));
    }

    @Operation(summary = "Update issue assignee", description = "Updates an issue's assignee.")
    @PatchMapping("/api/v1/issues/{id}/assignee")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> updateAssignee(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.updateAssignee(id, user.getId(), req)));
    }

    @Operation(summary = "Update issue priority", description = "Updates an issue's priority.")
    @PatchMapping("/api/v1/issues/{id}/priority")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<IssueResponse>> updatePriority(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(issueService.updatePriority(id, user.getId(), req)));
    }

    @Operation(summary = "Update issue dates", description = "Sets or clears the issue's start and due dates. Pass null to clear a date.")
    @PatchMapping("/api/v1/issues/{id}/dates")
    public ResponseEntity<ApiResponse<IssueResponse>> updateDates(
            @PathVariable UUID id,
            @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.updateDates(id, req)));
    }

    @Operation(summary = "Update issue parent", description = "Sets or clears the issue's parent. Pass a null parentId to detach.")
    @PatchMapping("/api/v1/issues/{id}/parent")
    public ResponseEntity<ApiResponse<IssueResponse>> updateParent(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.updateParent(id, user.getId(), req)));
    }

    @Operation(summary = "Update issue labels", description = "Replaces the set of labels attached to an issue.")
    @PutMapping("/api/v1/issues/{id}/labels")
    public ResponseEntity<ApiResponse<IssueResponse>> updateLabels(
            @PathVariable UUID id,
            @RequestBody UpdateIssueLabelsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.setLabels(id, req.getLabelIds())));
    }

    @Operation(summary = "Update issue release (fix version)", description = "Sets or clears the issue's fix version. Pass a null releaseId to clear.")
    @PatchMapping("/api/v1/issues/{id}/release")
    public ResponseEntity<ApiResponse<IssueResponse>> updateRelease(
            @PathVariable UUID id,
            @RequestBody SetIssueReleaseRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.setRelease(id, req.getReleaseId())));
    }

    @Operation(summary = "Delete issue", description = "Deletes an issue permanently.")
    @DeleteMapping("/api/v1/issues/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> delete(@PathVariable UUID id) {
        issueService.delete(id);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Issue deleted"));
    }

    @Operation(summary = "Get issue audit history", description = "Returns the chronological audit trail of changes made to an issue.")
    @GetMapping("/api/v1/issues/{id}/history")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<IssueHistoryResponse>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(historyService.getHistory(id)));
    }
}
