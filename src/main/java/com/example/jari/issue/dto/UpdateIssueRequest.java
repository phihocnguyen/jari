package com.example.jari.issue.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateIssueRequest {
    private String title;
    private String description;
    private UUID issueTypeId;
    private UUID statusId;
    private UUID priorityId;
    private UUID assigneeId;
    private UUID parentId;
    private BigDecimal storyPoints;
    private LocalDate dueDate;

    private String status;
    private String priority;
    private String type;
}

