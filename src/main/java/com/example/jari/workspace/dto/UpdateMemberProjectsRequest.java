package com.example.jari.workspace.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateMemberProjectsRequest {
    private List<UUID> projectIds;
}
