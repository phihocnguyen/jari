package com.example.jari.notification.dto;

/**
 * Carries an already-mapped notification (safe to read outside the transaction)
 * so the WebSocket push can happen after the enclosing transaction commits.
 */
public record NotificationCreatedEvent(NotificationResponse notification) {
}
