package com.example.jari.issue.dto;
import lombok.Data;
import java.util.UUID;

@Data
public class IssueFilterRequest {
    private UUID statusId;
    private UUID assigneeId;
    private UUID issueTypeId;
    private UUID priorityId;
    private UUID sprintId;
    private String keyword;
    private int page = 0;
    private int size = 20;
}
