package com.example.jari.issue.controller;

import com.example.jari.issue.dto.ProjectSummaryResponse;
import com.example.jari.issue.service.SummaryService;
import com.example.jari.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Summary", description = "Project summary dashboard data")
@RestController
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @Operation(
        summary = "Get project summary",
        description = "Returns aggregated metrics, status/priority/type breakdowns, " +
                      "team workload, recent activity, and epic progress for a project."
    )
    @GetMapping("/api/v1/projects/{projectId}/summary")
    public ResponseEntity<ApiResponse<ProjectSummaryResponse>> getSummary(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(summaryService.getSummary(projectId)));
    }
}
