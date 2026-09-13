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

@Tag(name = "Issues")
@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final IssueHistoryService historyService;

    @Operation(summary = "Create issue")
    @PostMapping("/api/v1/projects/{projectId}/issues")
    public ResponseEntity<ApiResponse<IssueResponse>> create(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateIssueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(issueService.create(projectId, user.getId(), req)));
    }

    @Operation(summary = "List issues with filter")
    @GetMapping("/api/v1/projects/{projectId}/issues")
    public ResponseEntity<ApiResponse<PageResponse<IssueResponse>>> list(
            @PathVariable UUID projectId,
            @ModelAttribute IssueFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.list(projectId, filter)));
    }

    @Operation(summary = "Get issue")
    @GetMapping("/api/v1/issues/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.get(id)));
    }

    @Operation(summary = "Update issue")
    @PutMapping("/api/v1/issues/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateIssueRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.update(id, user.getId(), req)));
    }

    @Operation(summary = "Delete issue")
    @DeleteMapping("/api/v1/issues/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        issueService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Issue deleted"));
    }

    @Operation(summary = "Get issue audit history")
    @GetMapping("/api/v1/issues/{id}/history")
    public ResponseEntity<ApiResponse<List<IssueHistoryResponse>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(historyService.getHistory(id)));
    }
}
