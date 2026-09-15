package com.example.jari.issue.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SetIssueReleaseRequest {
    private UUID releaseId;
}
