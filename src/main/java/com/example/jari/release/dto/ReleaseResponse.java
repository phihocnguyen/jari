package com.example.jari.release.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class ReleaseResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String status;
    private LocalDate releaseDate;
    private OffsetDateTime createdAt;
}
