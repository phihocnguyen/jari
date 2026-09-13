package com.example.jari.reference.controller;

import com.example.jari.reference.dto.ReferenceItemResponse;
import com.example.jari.reference.service.ReferenceService;
import com.example.jari.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reference Data", description = "Endpoints for retrieving read-only lookup data (issue types, statuses, priorities)")
@RestController
@RequestMapping("/api/v1/ref")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    @Operation(summary = "Get all issue types", description = "Returns the list of available issue types (e.g., Epic, Story, Task, Bug).")
    @GetMapping("/issue-types")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<ReferenceItemResponse>>> getIssueTypes() {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(referenceService.getIssueTypes()));
    }

    @Operation(summary = "Get all statuses", description = "Returns the list of all available statuses, including their category (TODO, IN_PROGRESS, DONE).")
    @GetMapping("/statuses")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<ReferenceItemResponse>>> getStatuses() {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(referenceService.getStatuses()));
    }

    @Operation(summary = "Get all priorities", description = "Returns the list of available priorities, ordered by level from highest to lowest.")
    @GetMapping("/priorities")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<List<ReferenceItemResponse>>> getPriorities() {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(referenceService.getPriorities()));
    }
}
