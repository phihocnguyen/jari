package com.example.jari.issue.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateIssueLabelsRequest {
    private List<UUID> labelIds;
}
