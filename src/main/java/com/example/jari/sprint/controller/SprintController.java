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

@Tag(name = "Sprints & Board", description = "Endpoints for managing sprints, assigning issues, and retrieving the Kanban board")
@RestController
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @Operation(summary = "Create sprint", description = "Creates a new planned sprint within a project.")
    @PostMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<SprintResponse>> create(
            @PathVariable UUID projectId, @Valid @RequestBody CreateSprintRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(sprintService.create(projectId, req)));
    }

    @Operation(summary = "List sprints", description = "Returns all sprints for a specific project, ordered by creation date.")
    @GetMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<SprintResponse>>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.list(projectId)));
    }

    @Operation(summary = "Get sprint", description = "Returns details for a single sprint.")
    @GetMapping("/api/v1/sprints/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<SprintResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.get(id)));
    }

    @Operation(summary = "Update sprint", description = "Updates sprint details such as name, goal, and dates.")
    @PutMapping("/api/v1/sprints/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<SprintResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSprintRequest req) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.update(id, req)));
    }

    @Operation(summary = "Delete sprint", description = "Deletes a sprint and moves its issues back to the backlog.")
    @DeleteMapping("/api/v1/sprints/{id}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> delete(@PathVariable UUID id) {
        sprintService.delete(id);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Sprint deleted successfully"));
    }

    @Operation(summary = "Start sprint", description = "Starts a planned sprint. A project can only have one active sprint at a time.")
    @PostMapping("/api/v1/sprints/{id}/start")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<SprintResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.start(id)));
    }

    @Operation(summary = "Complete sprint", description = "Marks an active sprint as completed.")
    @PostMapping("/api/v1/sprints/{id}/complete")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<SprintResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.complete(id)));
    }

    @Operation(summary = "Add issue to sprint", description = "Assigns an issue to a sprint, placing it at the end of the sprint backlog.")
    @PostMapping("/api/v1/sprints/{id}/issues")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> addIssue(
            @PathVariable UUID id, @Valid @RequestBody AddIssueToSprintRequest req) {
        sprintService.addIssue(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(com.example.jari.shared.response.ApiResponse.ok("Issue added to sprint"));
    }

    @Operation(summary = "Remove issue from sprint", description = "Removes an issue from a sprint.")
    @DeleteMapping("/api/v1/sprints/{id}/issues/{issueId}")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> removeIssue(
            @PathVariable UUID id, @PathVariable UUID issueId) {
        sprintService.removeIssue(id, issueId);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Issue removed from sprint"));
    }

    @Operation(summary = "Reorder issues in sprint", description = "Reorders the sprint backlog issues by a list of issue UUIDs in desired order.")
    @PutMapping("/api/v1/sprints/{id}/issues/reorder")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> reorderIssues(
            @PathVariable UUID id, @RequestBody ReorderSprintIssuesRequest req) {
        sprintService.reorderIssues(id, req.getIssueIds());
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Sprint issues reordered"));
    }

    @Operation(summary = "Update issue position in sprint", description = "Updates the position of a specific issue within a sprint.")
    @PatchMapping("/api/v1/sprints/{id}/issues/{issueId}/position")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> updateIssuePosition(
            @PathVariable UUID id, @PathVariable UUID issueId, @RequestBody UpdateIssuePositionRequest req) {
        sprintService.updateIssuePosition(id, issueId, req.getPosition());
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Issue position updated"));
    }

    @Operation(summary = "Get Kanban board", description = "Returns the board view for the currently active sprint in the project, grouped by status columns.")
    @GetMapping("/api/v1/projects/{projectId}/board")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<BoardColumnResponse>>> getBoard(@PathVariable UUID projectId) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(sprintService.getBoard(projectId)));
    }
}
