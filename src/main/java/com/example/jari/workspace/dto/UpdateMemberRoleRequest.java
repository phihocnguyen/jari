package com.example.jari.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotBlank
    private String roleName;
}
