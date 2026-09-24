package com.example.jari.report.controller;

import com.example.jari.report.dto.ProjectReportsResponse;
import com.example.jari.report.service.ReportService;
import com.example.jari.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reports", description = "Agile project reports: burndown, velocity, cumulative flow, created vs resolved")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
        summary = "Get project reports",
        description = "Returns burndown (active/selected sprint), velocity across recent sprints, "
            + "cumulative flow diagram, and created-vs-resolved series for the project."
    )
    @GetMapping("/api/v1/projects/{projectId}/reports")
    public ResponseEntity<ApiResponse<ProjectReportsResponse>> getReports(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer velocitySprints) {
        return ResponseEntity.ok(ApiResponse.ok(
            reportService.getReports(projectId, sprintId, days, velocitySprints)));
    }
}
