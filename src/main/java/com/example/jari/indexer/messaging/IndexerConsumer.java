package com.example.jari.indexer.messaging;

import com.example.jari.issue.search.IssueIndexService;
import com.example.jari.shared.config.RabbitMQConfig;
import com.example.jari.shared.messaging.IndexerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexerConsumer {

    private final IssueIndexService issueIndexService;

    @RabbitListener(queues = RabbitMQConfig.INDEXER_QUEUE)
    public void consume(IndexerMessage message) {
        if (message == null || message.kind() == null) {
            log.warn("Received empty indexer message, skipping");
            return;
        }
        log.debug("Indexer consumer handling kind={}", message.kind());
        switch (message.kind()) {
            case ISSUE_UPSERT -> issueIndexService.reindexAll(message.issueIds());
            case ISSUE_DELETE -> message.issueIds().forEach(issueIndexService::deleteFromIndex);
            case PROJECT_CHANGED -> issueIndexService.reindexByProject(message.projectId());
            case USER_CHANGED -> issueIndexService.reindexByUser(message.userId());
            default -> log.warn("Unknown indexer message kind: {}", message.kind());
        }
    }
}
