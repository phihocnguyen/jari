package com.example.jari.automation.controller;

import com.example.jari.automation.dto.AutomationLogResponse;
import com.example.jari.automation.dto.RunAutomationRequest;
import com.example.jari.automation.service.AutomationService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Automation", description = "Endpoints for viewing automation audit logs and executing automation rules")
@RestController
@RequestMapping("/api/v1/issues/{issueId}/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @Operation(summary = "List automation audit logs", description = "Returns history of automation rules executed for an issue.")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AutomationLogResponse>>> listLogs(@PathVariable UUID issueId) {
        return ResponseEntity.ok(ApiResponse.ok(automationService.listLogs(issueId)));
    }

    @Operation(summary = "Run automation rule", description = "Manually triggers an automation rule for an issue.")
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AutomationLogResponse>> runRule(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody RunAutomationRequest req) {
        UUID actorId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(automationService.runManualRule(issueId, actorId, req)));
    }
}
