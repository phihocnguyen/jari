package com.example.jari.workspace.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9_]+$", message = "workspace_key must be uppercase letters/numbers/underscore")
    private String workspaceKey;
    private String description;
}
