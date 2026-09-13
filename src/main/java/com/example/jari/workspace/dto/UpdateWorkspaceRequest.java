package com.example.jari.workspace.dto;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWorkspaceRequest {
    @Size(max = 100) private String name;
    private String description;
}
