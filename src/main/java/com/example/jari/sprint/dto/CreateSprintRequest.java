package com.example.jari.sprint.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class CreateSprintRequest {
    @NotBlank @Size(max = 100) private String name;
    private String goal;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
}
