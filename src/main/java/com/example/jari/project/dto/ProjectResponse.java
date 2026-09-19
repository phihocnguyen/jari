package com.example.jari.project.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class ProjectResponse {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private String projectKey;
    private String description;
    private UUID leadId;
    private String leadName;
    private String leadEmail;
    private String leadAvatarUrl;
    private String defaultAssignee;
    private String projectType;
    private String status;
    private String avatarIcon;
    private String avatarColor;
    private OffsetDateTime createdAt;
}
