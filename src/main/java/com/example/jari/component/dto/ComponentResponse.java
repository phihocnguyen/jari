package com.example.jari.component.dto;

import com.example.jari.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private UserResponse lead;
    private long issueCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
