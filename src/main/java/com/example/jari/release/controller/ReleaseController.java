package com.example.jari.release.controller;

import com.example.jari.release.dto.CreateReleaseRequest;
import com.example.jari.release.dto.ReleaseResponse;
import com.example.jari.release.service.ReleaseService;
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

@Tag(name = "Releases", description = "Endpoints for managing project releases (fix versions)")
@RestController
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @Operation(summary = "List project releases", description = "Returns all releases of a project, newest first.")
    @GetMapping("/api/v1/projects/{projectId}/releases")
    public ResponseEntity<ApiResponse<List<ReleaseResponse>>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(releaseService.list(projectId)));
    }

    @Operation(summary = "Get release", description = "Returns details for a single release.")
    @GetMapping("/api/v1/releases/{id}")
    public ResponseEntity<ApiResponse<ReleaseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(releaseService.get(id)));
    }

    @Operation(summary = "Create release", description = "Creates a new release (fix version) in a project.")
    @PostMapping("/api/v1/projects/{projectId}/releases")
    public ResponseEntity<ApiResponse<ReleaseResponse>> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateReleaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(releaseService.create(projectId, req)));
    }

    @Operation(summary = "Update release", description = "Updates release details such as name, description, release date, and status.")
    @PutMapping("/api/v1/releases/{id}")
    public ResponseEntity<ApiResponse<ReleaseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody com.example.jari.release.dto.UpdateReleaseRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(releaseService.update(id, req)));
    }

    @Operation(summary = "Delete release", description = "Deletes a release and unlinks any associated issues.")
    @DeleteMapping("/api/v1/releases/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        releaseService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Release deleted successfully"));
    }
}
