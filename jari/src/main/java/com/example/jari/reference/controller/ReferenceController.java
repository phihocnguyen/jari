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

@Tag(name = "Reference Data", description = "Read-only lookup data")
@RestController
@RequestMapping("/api/v1/ref")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    @Operation(summary = "Get all issue types")
    @GetMapping("/issue-types")
    public ResponseEntity<ApiResponse<List<ReferenceItemResponse>>> getIssueTypes() {
        return ResponseEntity.ok(ApiResponse.ok(referenceService.getIssueTypes()));
    }

    @Operation(summary = "Get all statuses")
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<ReferenceItemResponse>>> getStatuses() {
        return ResponseEntity.ok(ApiResponse.ok(referenceService.getStatuses()));
    }

    @Operation(summary = "Get all priorities")
    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse<List<ReferenceItemResponse>>> getPriorities() {
        return ResponseEntity.ok(ApiResponse.ok(referenceService.getPriorities()));
    }
}
