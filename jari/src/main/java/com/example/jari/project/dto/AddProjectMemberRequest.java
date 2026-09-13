package com.example.jari.project.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddProjectMemberRequest {
    @NotNull private UUID userId;
    @NotBlank private String roleName;
}
