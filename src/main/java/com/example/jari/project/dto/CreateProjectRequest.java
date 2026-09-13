package com.example.jari.project.dto;
import com.example.jari.project.entity.ProjectType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateProjectRequest {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9_]+$") private String projectKey;
    private String description;
    private UUID leadId;
    private ProjectType projectType = ProjectType.SOFTWARE;
}
