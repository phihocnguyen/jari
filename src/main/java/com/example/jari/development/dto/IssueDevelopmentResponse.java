package com.example.jari.development.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDevelopmentResponse {
    private UUID id;
    private UUID issueId;
    private String type;
    private String repoUrl;
    private String title;
    private String url;
    private String status;
    private String author;
    private OffsetDateTime createdAt;
}
