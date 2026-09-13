package com.example.jari.notification.messaging;

import com.example.jari.notification.dto.NotificationPayload;
import com.example.jari.shared.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationPayload payload) {
        log.debug("Received notification for user: {}", payload.getTargetUserId());
        if (payload.getTargetUserId() != null) {
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + payload.getTargetUserId(),
                payload
            );
        }
    }
}
