package com.example.jari.issue.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateIssueRequest {
    @NotBlank @Size(max = 255) private String title;
    private String description;
    @NotNull private UUID issueTypeId;
    @NotNull private UUID statusId;
    @NotNull private UUID priorityId;
    private UUID assigneeId;
    private UUID parentId;
    private BigDecimal storyPoints;
    private LocalDate dueDate;
}
