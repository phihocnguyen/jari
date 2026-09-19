package com.example.jari.component.controller;

import com.example.jari.component.dto.ComponentResponse;
import com.example.jari.component.dto.CreateComponentRequest;
import com.example.jari.component.dto.UpdateComponentRequest;
import com.example.jari.component.service.ComponentService;
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

@Tag(name = "Components", description = "Endpoints for managing project components")
@RestController
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;

    @Operation(summary = "List project components", description = "Returns all components belonging to a project.")
    @GetMapping("/api/v1/projects/{projectId}/components")
    public ResponseEntity<ApiResponse<List<ComponentResponse>>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(componentService.list(projectId)));
    }

    @Operation(summary = "Get component", description = "Returns a single component by its ID.")
    @GetMapping("/api/v1/components/{id}")
    public ResponseEntity<ApiResponse<ComponentResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(componentService.get(id)));
    }

    @Operation(summary = "Create component", description = "Creates a new component in a project.")
    @PostMapping("/api/v1/projects/{projectId}/components")
    public ResponseEntity<ApiResponse<ComponentResponse>> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateComponentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(componentService.create(projectId, req)));
    }

    @Operation(summary = "Update component", description = "Updates details of an existing component.")
    @PutMapping("/api/v1/components/{id}")
    public ResponseEntity<ApiResponse<ComponentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComponentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(componentService.update(id, req)));
    }

    @Operation(summary = "Delete component", description = "Deletes a component.")
    @DeleteMapping("/api/v1/components/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        componentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Component deleted successfully"));
    }
}
