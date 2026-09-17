package com.example.jari.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProjectMemberRoleRequest {
    @NotBlank
    private String roleName;
}
