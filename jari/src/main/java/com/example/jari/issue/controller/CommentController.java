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

@Tag(name = "Comments")
@RestController
@RequestMapping("/api/v1/issues/{issueId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "List comments for issue")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> list(@PathVariable UUID issueId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.list(issueId)));
    }

    @Operation(summary = "Create comment")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(commentService.create(issueId, user.getId(), req)));
    }

    @Operation(summary = "Update comment")
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CommentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.update(commentId, user.getId(), req)));
    }

    @Operation(summary = "Delete comment (soft)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        commentService.delete(commentId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted"));
    }
}
