package com.example.jari.project.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class ProjectMemberResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String roleName;
    private OffsetDateTime joinedAt;
}
