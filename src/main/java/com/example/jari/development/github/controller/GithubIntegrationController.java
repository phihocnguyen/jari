package com.example.jari.development.github.controller;

import com.example.jari.development.github.dto.MapGithubRepoRequest;
import com.example.jari.development.github.service.GithubApiClient;
import com.example.jari.development.github.service.GithubInstallationService;
import com.example.jari.shared.config.AppProperties;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "GitHub Integration", description = "Connect GitHub App installations and map repos to projects")
@RestController
@RequiredArgsConstructor
@Slf4j
public class GithubIntegrationController {

    private final GithubInstallationService installationService;
    private final GithubApiClient githubApiClient;
    private final AppProperties appProperties;

    @Operation(summary = "Get GitHub App install URL for a workspace")
    @GetMapping("/api/v1/workspaces/{workspaceId}/github/install-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> installUrl(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable UUID workspaceId
    ) {
        UUID userId = user != null ? user.getId() : null;
        String url = installationService.buildInstallUrl(userId, workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url)));
    }

    @Operation(summary = "List GitHub installations and repos for a workspace")
    @GetMapping("/api/v1/workspaces/{workspaceId}/github/installations")
    public ResponseEntity<ApiResponse<List<GithubInstallationService.InstallationView>>> list(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable UUID workspaceId
    ) {
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(installationService.listInstallations(userId, workspaceId)));
    }

    @Operation(summary = "Map a GitHub repo to a Jari project (or unmap with null projectId)")
    @PutMapping("/api/v1/workspaces/{workspaceId}/github/repos/{repoId}")
    public ResponseEntity<ApiResponse<GithubInstallationService.RepoView>> mapRepo(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable UUID workspaceId,
        @PathVariable UUID repoId,
        @Valid @RequestBody MapGithubRepoRequest req
    ) {
        UUID userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
            installationService.mapRepoToProject(userId, workspaceId, repoId, req.getProjectId())));
    }

    @Operation(summary = "Disconnect a GitHub installation from the workspace")
    @DeleteMapping("/api/v1/workspaces/{workspaceId}/github/installations/{installationId}")
    public ResponseEntity<ApiResponse<Void>> disconnect(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable UUID workspaceId,
        @PathVariable UUID installationId
    ) {
        UUID userId = user != null ? user.getId() : null;
        installationService.disconnectInstallation(userId, workspaceId, installationId);
        return ResponseEntity.ok(ApiResponse.ok("GitHub installation disconnected"));
    }

    /**
     * GitHub App Setup URL callback after install.
     * Configure as App "Setup URL": {backend}/api/v1/github/setup
     */
    @GetMapping("/api/v1/github/setup")
    public ResponseEntity<Void> setupCallback(
        @RequestParam("installation_id") long installationId,
        @RequestParam(value = "setup_action", required = false) String setupAction,
        @RequestParam(value = "state", required = false) String state
    ) {
        String frontend = appProperties.getGithub().getFrontendBaseUrl();
        try {
            if (state == null || state.isBlank()) {
                log.warn("GitHub setup callback missing state for installation {}", installationId);
                return redirect(frontend + "/workspaces?github=missing_state");
            }
            JsonNode installation = githubApiClient.getInstallation(installationId);
            JsonNode account = installation.path("account");
            UUID workspaceId = installationService.linkInstallationFromSetup(state, installationId, account);
            String redirectUrl = UriComponentsBuilder
                .fromUriString(frontend + "/workspaces/" + workspaceId + "/settings")
                .queryParam("tab", "integrations")
                .queryParam("github", "connected")
                .build()
                .toUriString();
            return redirect(redirectUrl);
        } catch (Exception e) {
            log.error("GitHub setup callback failed: {}", e.getMessage());
            return redirect(frontend + "/workspaces?github=error");
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, url)
            .build();
    }
}
