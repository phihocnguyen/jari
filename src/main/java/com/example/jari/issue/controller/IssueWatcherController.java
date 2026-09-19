package com.example.jari.issue.controller;

import com.example.jari.issue.dto.WatchersResponse;
import com.example.jari.issue.service.IssueWatcherService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Issue Watchers", description = "Endpoints for managing watchers on issues")
@RestController
@RequestMapping("/api/v1/issues/{issueId}/watchers")
@RequiredArgsConstructor
public class IssueWatcherController {

    private final IssueWatcherService watcherService;

    @Operation(summary = "Get watchers for issue", description = "Returns watchers count, whether current user is watching, and list of watching users.")
    @GetMapping
    public ResponseEntity<ApiResponse<WatchersResponse>> getWatchers(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user) {
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(watcherService.getWatchers(issueId, userId)));
    }

    @Operation(summary = "Watch an issue", description = "Adds the current user to the issue's watcher list.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> watchIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user) {
        watcherService.watchIssue(issueId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Issue watched successfully"));
    }

    @Operation(summary = "Unwatch an issue", description = "Removes the current user from the issue's watcher list.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unwatchIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user) {
        watcherService.unwatchIssue(issueId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Issue unwatched successfully"));
    }
}
