package com.example.jari.issue.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class IssueResponse {
    private UUID id;
    private String issueKey;
    private String title;
    private String description;
    private UUID projectId;
    private String issueType;
    private String status;
    private String statusCategory;
    private String priority;
    private UUID reporterId;
    private String reporterName;
    private UUID assigneeId;
    private String assigneeName;
    private UUID parentId;
    private UUID releaseId;
    private String releaseName;
    private UUID sprintId;
    private String sprintName;
    private BigDecimal storyPoints;
    private LocalDate startDate;
    private LocalDate dueDate;
    private List<LabelResponse> labels;
    private List<com.example.jari.component.dto.ComponentResponse> components;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
