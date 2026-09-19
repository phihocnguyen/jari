package com.example.jari.automation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationLogResponse {
    private UUID id;
    private UUID issueId;
    private String ruleName;
    private String status;
    private String description;
    private OffsetDateTime executedAt;
}
