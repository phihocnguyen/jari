package com.example.jari.sprint.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class SprintResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String goal;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String status;
    private OffsetDateTime createdAt;
}
