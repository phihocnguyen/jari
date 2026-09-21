package com.example.jari.issue.search;

import java.util.UUID;

/**
 * Bắn khi user đổi displayName (reporter/assignee name trong issue index).
 * IndexerConsumer sẽ reindex lại các issue liên quan qua RabbitMQ.
 */
public record UserIndexChangedEvent(UUID userId) {
}
