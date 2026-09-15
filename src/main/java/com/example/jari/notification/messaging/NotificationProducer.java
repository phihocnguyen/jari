package com.example.jari.notification.messaging;

import com.example.jari.notification.dto.NotificationPayload;
import com.example.jari.shared.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(NotificationPayload payload) {
        if (payload.getTimestamp() == null) payload.setTimestamp(Instant.now());
        log.debug("Publishing notification: {}", payload.getType());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.NOTIFICATION_EXCHANGE,
            "notification.user",
            payload
        );
    }
}
