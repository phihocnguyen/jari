package com.example.jari.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkspaceMemberResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String roleName;
    private OffsetDateTime joinedAt;
    private List<UUID> projectIds;
    private List<String> projectNames;
}
