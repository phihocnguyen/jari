package com.example.jari.project.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddProjectMemberRequest {
    private UUID userId;
    private String email;
    @NotBlank private String roleName;
}
