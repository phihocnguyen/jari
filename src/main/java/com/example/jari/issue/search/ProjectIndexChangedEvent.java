package com.example.jari.issue.search;

import java.util.UUID;

/**
 * Bắn khi project đổi thông tin xuất hiện trong issue index (name/key).
 * IndexerConsumer sẽ reindex lại toàn bộ issue của project qua RabbitMQ.
 */
public record ProjectIndexChangedEvent(UUID projectId) {
}
