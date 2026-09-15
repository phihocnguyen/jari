package com.example.jari.sprint.dto;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UpdateSprintRequest {
    private String name;
    private String goal;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
}
