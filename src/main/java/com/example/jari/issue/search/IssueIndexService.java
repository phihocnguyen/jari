package com.example.jari.issue.search;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Elasticsearch write-through indexing invoked by {@link com.example.jari.indexer.messaging.IndexerConsumer}.
 * Logstash indexer poll (30s) remains a safety net for missed writes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueIndexService {

    private static final IndexCoordinates JARI_ISSUES = IndexCoordinates.of("jari-issues");

    private final ElasticsearchOperations operations;
    private final IssueRepository issueRepository;
    private final TransactionTemplate transactionTemplate;

    public void reindexAll(List<UUID> issueIds) {
        issueIds.forEach(this::reindex);
    }

    public void reindex(UUID issueId) {
        try {
            IssueSearchDocument doc = transactionTemplate.execute(status ->
                issueRepository.findById(issueId).map(this::toDocument).orElse(null));
            if (doc == null) {
                deleteFromIndex(issueId);
                return;
            }
            operations.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index issue {} into Elasticsearch, Logstash indexer will catch up: {}",
                issueId, e.getMessage());
        }
    }

    public void deleteFromIndex(UUID issueId) {
        try {
            operations.delete(issueId.toString(), JARI_ISSUES);
        } catch (Exception e) {
            log.warn("Failed to delete issue {} from Elasticsearch index, Logstash indexer will catch up: {}",
                issueId, e.getMessage());
        }
    }

    public void reindexByProject(UUID projectId) {
        reindexAll(issueRepository.findIdsByProjectId(projectId));
    }

    public void reindexByUser(UUID userId) {
        reindexAll(issueRepository.findIdsByUserId(userId));
    }

    private IssueSearchDocument toDocument(Issue issue) {
        IssueSearchDocument doc = new IssueSearchDocument();
        doc.setId(issue.getId().toString());
        doc.setIssueKey(issue.getIssueKey());
        doc.setIssueKeyLower(lower(issue.getIssueKey()));
        doc.setTitleLower(lower(issue.getTitle()));
        doc.setProjectId(issue.getProject() != null ? issue.getProject().getId().toString() : null);
        doc.setStatusId(issue.getStatus() != null ? issue.getStatus().getId().toString() : null);
        doc.setAssigneeId(issue.getAssignee() != null ? issue.getAssignee().getId().toString() : null);
        doc.setIssueTypeId(issue.getIssueType() != null ? issue.getIssueType().getId().toString() : null);
        doc.setPriorityId(issue.getPriority() != null ? issue.getPriority().getId().toString() : null);
        doc.setSprintIds(issue.getSprintIssues() == null ? List.of() : issue.getSprintIssues().stream()
            .map(si -> si.getSprint() != null ? si.getSprint().getId().toString() : null)
            .filter(Objects::nonNull)
            .toList());
        doc.setPosition(issue.getPosition() != null ? issue.getPosition().doubleValue() : null);
        doc.setCreatedAt(issue.getCreatedAt());
        return doc;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }
}
