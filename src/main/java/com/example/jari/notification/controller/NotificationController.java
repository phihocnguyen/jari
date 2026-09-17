package com.example.jari.notification.controller;

import com.example.jari.notification.dto.NotificationResponse;
import com.example.jari.notification.service.NotificationService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications", description = "Endpoints for the current user's notification inbox (backed by real-time WebSocket pushes)")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List my notifications", description = "Returns the 50 most recent notifications for the current user.")
    @GetMapping("/api/v1/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listForUser(user.getId())));
    }

    @Operation(summary = "Unread count", description = "Returns the number of unread notifications for the current user.")
    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.unreadCount(user.getId())));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read.")
    @PutMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.markRead(user.getId(), id)));
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks every unread notification of the current user as read.")
    @PutMapping("/api/v1/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}
