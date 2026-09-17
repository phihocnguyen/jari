package com.example.jari.notification.messaging;

import com.example.jari.notification.dto.NotificationCreatedEvent;
import com.example.jari.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes committed notifications to the STOMP broker.
 * Destination: /topic/notifications/{userId} — subscribing to another user's
 * topic is rejected by WebSocketAuthChannelInterceptor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationResponse n = event.notification();
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + n.getTargetUserId(), n);
        } catch (Exception e) {
            log.error("Failed to push notification {} over WebSocket: {}", n.getId(), e.getMessage(), e);
        }
    }
}
