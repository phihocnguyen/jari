package com.example.jari.sprint.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddIssueToSprintRequest {
    @NotNull private UUID issueId;
}
