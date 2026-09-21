package com.example.jari.shared.messaging;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Unified payload for the indexer consumer queue.
 */
public record IndexerMessage(
    Kind kind,
    List<UUID> issueIds,
    UUID projectId,
    UUID userId
) implements Serializable {

    public enum Kind {
        ISSUE_UPSERT,
        ISSUE_DELETE,
        PROJECT_CHANGED,
        USER_CHANGED
    }

    public static IndexerMessage issueUpsert(List<UUID> issueIds) {
        return new IndexerMessage(Kind.ISSUE_UPSERT, issueIds, null, null);
    }

    public static IndexerMessage issueUpsert(UUID issueId) {
        return issueUpsert(List.of(issueId));
    }

    public static IndexerMessage issueDelete(UUID issueId) {
        return new IndexerMessage(Kind.ISSUE_DELETE, List.of(issueId), null, null);
    }

    public static IndexerMessage projectChanged(UUID projectId) {
        return new IndexerMessage(Kind.PROJECT_CHANGED, null, projectId, null);
    }

    public static IndexerMessage userChanged(UUID userId) {
        return new IndexerMessage(Kind.USER_CHANGED, null, null, userId);
    }
}
