package com.example.jari.project.presence.controller;

import com.example.jari.project.presence.dto.OnlineUserResponse;
import com.example.jari.project.presence.service.ProjectPresenceService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Project Presence", description = "Endpoints for tracking active online project viewers in real time")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/presence")
@RequiredArgsConstructor
public class ProjectPresenceController {

    private final ProjectPresenceService presenceService;
    private final UserRepository userRepository;

    @Operation(summary = "Get online users for project", description = "Returns active users currently viewing the project.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OnlineUserResponse>>> getOnlineUsers(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(presenceService.getOnlineUsers(projectId)));
    }

    @Operation(summary = "Project presence heartbeat", description = "Signals that current user is actively viewing the project.")
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            String displayName = userDetails.getUsername();
            String avatarUrl = null;
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            if (user != null) {
                displayName = user.getDisplayName();
                avatarUrl = user.getAvatarUrl();
            }
            presenceService.heartbeat(projectId, userDetails.getId(), displayName, avatarUrl);
        }
        return ResponseEntity.ok(ApiResponse.ok("Heartbeat received"));
    }

    @Operation(summary = "Leave project presence", description = "Removes current user from active viewers list when navigating away.")
    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leave(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            presenceService.leave(projectId, userDetails.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok("Left project view"));
    }
}
