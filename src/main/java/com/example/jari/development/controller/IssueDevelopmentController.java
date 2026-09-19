package com.example.jari.development.controller;

import com.example.jari.development.dto.CreateDevelopmentRequest;
import com.example.jari.development.dto.IssueDevelopmentResponse;
import com.example.jari.development.service.IssueDevelopmentService;
import com.example.jari.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Issue Developments", description = "Endpoints for linking Git commits, branches, and pull requests to issues")
@RestController
@RequiredArgsConstructor
public class IssueDevelopmentController {

    private final IssueDevelopmentService developmentService;

    @Operation(summary = "List developments for issue", description = "Returns all linked commits, branches, and PRs for an issue.")
    @GetMapping("/api/v1/issues/{issueId}/developments")
    public ResponseEntity<ApiResponse<List<IssueDevelopmentResponse>>> list(@PathVariable UUID issueId) {
        return ResponseEntity.ok(ApiResponse.ok(developmentService.list(issueId)));
    }

    @Operation(summary = "Link development to issue", description = "Links a new commit, branch, or PR to an issue.")
    @PostMapping("/api/v1/issues/{issueId}/developments")
    public ResponseEntity<ApiResponse<IssueDevelopmentResponse>> create(
            @PathVariable UUID issueId,
            @Valid @RequestBody CreateDevelopmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(developmentService.create(issueId, req)));
    }

    @Operation(summary = "Delete development link", description = "Unlinks a commit, branch, or PR from an issue.")
    @DeleteMapping("/api/v1/developments/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        developmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Development link removed successfully"));
    }
}
