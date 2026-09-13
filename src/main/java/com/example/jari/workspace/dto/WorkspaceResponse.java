package com.example.jari.workspace.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private String workspaceKey;
    private String description;
    private UUID ownerId;
    private OffsetDateTime createdAt;
}
