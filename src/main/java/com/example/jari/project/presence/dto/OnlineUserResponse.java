package com.example.jari.project.presence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserResponse {
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private Instant lastSeen;
}
