package com.example.jari.issue.search;

import java.util.List;
import java.util.UUID;

/**
 * Bắn ra trong transaction (ApplicationEventPublisher); RabbitMQEventBridge
 * chuyển sang RabbitMQ sau commit, IndexerConsumer gọi IssueIndexService
 * index vào Elasticsearch. Nếu write lỗi chỉ log warn — Logstash poll 30s hàn gắn lại.
 */
public record IssueIndexEvent(List<UUID> issueIds, boolean deleted) {

    public static IssueIndexEvent upsert(UUID issueId) {
        return new IssueIndexEvent(List.of(issueId), false);
    }

    public static IssueIndexEvent upsert(List<UUID> issueIds) {
        return new IssueIndexEvent(issueIds, false);
    }

    public static IssueIndexEvent delete(UUID issueId) {
        return new IssueIndexEvent(List.of(issueId), true);
    }
}
