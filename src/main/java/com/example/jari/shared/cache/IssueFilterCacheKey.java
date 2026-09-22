package com.example.jari.shared.cache;

import com.example.jari.issue.dto.IssueFilterRequest;

import java.util.Objects;
import java.util.UUID;

public final class IssueFilterCacheKey {

    private IssueFilterCacheKey() {
    }

    public static String of(UUID projectId, IssueFilterRequest filter) {
        return projectId + ":"
            + filter.getPage() + ":"
            + filter.getSize() + ":"
            + Objects.hash(
                filter.getStatusId(),
                filter.getAssigneeId(),
                filter.getIssueTypeId(),
                filter.getPriorityId(),
                filter.getSprintId(),
                normalizeKeyword(filter.getKeyword()));
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }
}
