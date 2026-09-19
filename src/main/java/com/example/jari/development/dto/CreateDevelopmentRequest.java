package com.example.jari.development.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDevelopmentRequest {
    @NotBlank(message = "Type is required")
    private String type; // 'COMMIT', 'PULL_REQUEST', 'BRANCH'

    private String repoUrl;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "URL is required")
    private String url;

    private String status;

    private String author;
}
