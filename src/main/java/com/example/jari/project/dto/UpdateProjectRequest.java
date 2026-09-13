package com.example.jari.project.dto;
import com.example.jari.project.entity.ProjectStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class UpdateProjectRequest {
    @Size(max = 100) private String name;
    private String description;
    private UUID leadId;
    private ProjectStatus status;
}
