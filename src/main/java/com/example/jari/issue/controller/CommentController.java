package com.example.jari.issue.controller;

import com.example.jari.issue.dto.CommentRequest;
import com.example.jari.issue.dto.CommentResponse;
import com.example.jari.issue.service.CommentService;
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

@Tag(name = "Comments", description = "Endpoints for managing comments on issues")
@RestController
@RequestMapping("/api/v1/issues/{issueId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "List comments for issue", description = "Returns all active (non-deleted) comments for a given issue, ordered chronologically.")
    @GetMapping
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<CommentResponse>>> list(@PathVariable UUID issueId) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(commentService.list(issueId)));
    }

    @Operation(summary = "Create comment", description = "Adds a new comment to an issue.")
    @PostMapping
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<CommentResponse>> create(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(commentService.create(issueId, user.getId(), req)));
    }

    @Operation(summary = "Update comment", description = "Updates an existing comment. Only the author can perform this action.")
    @PutMapping("/{commentId}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<CommentResponse>> update(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(commentService.update(commentId, user.getId(), req)));
    }

    @Operation(summary = "Delete comment", description = "Soft-deletes a comment. Only the author can perform this action.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> delete(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        commentService.delete(commentId, user.getId());
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Comment deleted"));
    }
}
