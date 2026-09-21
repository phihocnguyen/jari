package com.example.jari.shared.messaging;

import com.example.jari.issue.search.IssueIndexEvent;
import com.example.jari.issue.search.ProjectIndexChangedEvent;
import com.example.jari.issue.search.UserIndexChangedEvent;
import com.example.jari.notification.dto.NotificationCreatedEvent;
import com.example.jari.notification.dto.NotificationResponse;
import com.example.jari.shared.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes domain events to RabbitMQ after the enclosing transaction commits.
 * Downstream consumers handle notification push and Elasticsearch indexing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQEventBridge {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationResponse n = event.notification();
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                RabbitMQConfig.RK_NOTIFICATION_USER,
                n
            );
        } catch (Exception e) {
            log.error("Failed to publish notification {} to RabbitMQ: {}", n.getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueIndexEvent(IssueIndexEvent event) {
        try {
            IndexerMessage message = event.deleted()
                ? IndexerMessage.issueDelete(event.issueIds().get(0))
                : IndexerMessage.issueUpsert(event.issueIds());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                RabbitMQConfig.RK_INDEXER_ISSUE,
                message
            );
        } catch (Exception e) {
            log.error("Failed to publish issue index event to RabbitMQ: {}", e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectIndexChanged(ProjectIndexChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                RabbitMQConfig.RK_INDEXER_PROJECT,
                IndexerMessage.projectChanged(event.projectId())
            );
        } catch (Exception e) {
            log.error("Failed to publish project index event to RabbitMQ: {}", e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserIndexChanged(UserIndexChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                RabbitMQConfig.RK_INDEXER_USER,
                IndexerMessage.userChanged(event.userId())
            );
        } catch (Exception e) {
            log.error("Failed to publish user index event to RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
