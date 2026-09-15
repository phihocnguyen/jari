package com.example.jari.issue.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class IssueHistoryResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String field;
    private String oldValue;
    private String newValue;
    private OffsetDateTime createdAt;
}
