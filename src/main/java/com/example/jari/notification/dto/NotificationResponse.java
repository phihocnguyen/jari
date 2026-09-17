package com.example.jari.notification.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String type;
    private UUID targetUserId;
    private UUID issueId;
    private String issueKey;
    private UUID projectId;
    private String projectName;
    private String message;
    private boolean read;
    private Instant createdAt;
}
