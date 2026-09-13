package com.example.jari.sprint.controller;

import com.example.jari.shared.response.ApiResponse;
import com.example.jari.sprint.dto.*;
import com.example.jari.sprint.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sprints & Board")
@RestController
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @Operation(summary = "Create sprint")
    @PostMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<ApiResponse<SprintResponse>> create(
            @PathVariable UUID projectId, @Valid @RequestBody CreateSprintRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(sprintService.create(projectId, req)));
    }

    @Operation(summary = "List sprints")
    @GetMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.list(projectId)));
    }

    @Operation(summary = "Update sprint")
    @PutMapping("/api/v1/sprints/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSprintRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.update(id, req)));
    }

    @Operation(summary = "Start sprint")
    @PostMapping("/api/v1/sprints/{id}/start")
    public ResponseEntity<ApiResponse<SprintResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.start(id)));
    }

    @Operation(summary = "Complete sprint")
    @PostMapping("/api/v1/sprints/{id}/complete")
    public ResponseEntity<ApiResponse<SprintResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.complete(id)));
    }

    @Operation(summary = "Add issue to sprint")
    @PostMapping("/api/v1/sprints/{id}/issues")
    public ResponseEntity<ApiResponse<Void>> addIssue(
            @PathVariable UUID id, @Valid @RequestBody AddIssueToSprintRequest req) {
        sprintService.addIssue(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Issue added to sprint"));
    }

    @Operation(summary = "Remove issue from sprint")
    @DeleteMapping("/api/v1/sprints/{id}/issues/{issueId}")
    public ResponseEntity<ApiResponse<Void>> removeIssue(
            @PathVariable UUID id, @PathVariable UUID issueId) {
        sprintService.removeIssue(id, issueId);
        return ResponseEntity.ok(ApiResponse.ok("Issue removed from sprint"));
    }

    @Operation(summary = "Get Kanban board (active sprint)")
    @GetMapping("/api/v1/projects/{projectId}/board")
    public ResponseEntity<ApiResponse<List<BoardColumnResponse>>> getBoard(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.getBoard(projectId)));
    }
}
