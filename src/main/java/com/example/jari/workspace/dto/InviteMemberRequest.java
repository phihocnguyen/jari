package com.example.jari.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class InviteMemberRequest {
    private UUID userId;
    private String email;
    @NotBlank private String roleName;
    private List<UUID> projectIds;
}
