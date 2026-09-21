package com.example.jari.notification.messaging;

import com.example.jari.notification.dto.NotificationResponse;
import com.example.jari.shared.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes notification events from RabbitMQ and pushes them to connected
 * clients via STOMP (/topic/notifications/{userId}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationResponse notification) {
        if (notification == null || notification.getTargetUserId() == null) {
            log.warn("Received notification without targetUserId, skipping");
            return;
        }
        log.debug("Pushing notification {} to user {}", notification.getId(), notification.getTargetUserId());
        try {
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getTargetUserId(),
                notification
            );
        } catch (Exception e) {
            log.error("Failed to push notification {} over WebSocket: {}",
                notification.getId(), e.getMessage(), e);
        }
    }
}
