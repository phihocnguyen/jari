package com.example.jari.issue.controller;

import com.example.jari.issue.dto.CreateLabelRequest;
import com.example.jari.issue.dto.LabelResponse;
import com.example.jari.issue.service.LabelService;
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

@Tag(name = "Labels", description = "Endpoints for managing project labels")
@RestController
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @Operation(summary = "List project labels", description = "Returns all labels available in a project.")
    @GetMapping("/api/v1/projects/{projectId}/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(labelService.list(projectId)));
    }

    @Operation(summary = "Create label", description = "Creates a label in a project. Returns the existing label if the name already exists (case-insensitive).")
    @PostMapping("/api/v1/projects/{projectId}/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLabelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(labelService.create(projectId, req)));
    }
}
