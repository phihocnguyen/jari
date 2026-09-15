package com.example.jari.notification.dto;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPayload implements Serializable {
    private String type;           // ISSUE_ASSIGNED, ISSUE_UPDATED, COMMENT_ADDED...
    private UUID targetUserId;
    private UUID issueId;
    private String issueKey;
    private String message;
    private Instant timestamp;
}
