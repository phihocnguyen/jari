package com.example.jari.issue.spec;

import com.example.jari.issue.entity.Issue;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IssueSpecificationTest {

    @Mock private Root<Issue> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<Object> projectPath;
    @Mock private Path<Object> projectIdPath;
    @Mock private Path<Object> statusPath;
    @Mock private Path<Object> statusIdPath;
    @Mock private Path<Object> assigneePath;
    @Mock private Path<Object> assigneeIdPath;
    @Mock private Path<Object> issueTypePath;
    @Mock private Path<Object> issueTypeIdPath;
    @Mock private Path<Object> priorityPath;
    @Mock private Path<Object> priorityIdPath;
    @Mock private Path<Object> titlePath;
    @Mock private Path<Object> issueKeyPath;
    @Mock private Join<Object, Object> sprintIssuesJoin;
    @Mock private Path<Object> sprintPath;
    @Mock private Path<Object> sprintIdPath;
    @Mock private Predicate predicate;
    @Mock private Predicate orPredicate;
    @Mock private Expression<String> lowerExpression;

    @BeforeEach
    void setUpPaths() {
        when(root.get("project")).thenReturn(projectPath);
        when(projectPath.get("id")).thenReturn(projectIdPath);
        when(root.get("status")).thenReturn(statusPath);
        when(statusPath.get("id")).thenReturn(statusIdPath);
        when(root.get("assignee")).thenReturn(assigneePath);
        when(assigneePath.get("id")).thenReturn(assigneeIdPath);
        when(root.get("issueType")).thenReturn(issueTypePath);
        when(issueTypePath.get("id")).thenReturn(issueTypeIdPath);
        when(root.get("priority")).thenReturn(priorityPath);
        when(priorityPath.get("id")).thenReturn(priorityIdPath);
        when(root.get("title")).thenReturn(titlePath);
        when(root.get("issueKey")).thenReturn(issueKeyPath);
        when(root.join("sprintIssues", JoinType.INNER)).thenReturn(sprintIssuesJoin);
        when(sprintIssuesJoin.get("sprint")).thenReturn(sprintPath);
        when(sprintPath.get("id")).thenReturn(sprintIdPath);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.lower(any())).thenReturn(lowerExpression);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.or(any(), any())).thenReturn(orPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    void filter_withAllParameters_buildsPredicates() {
        UUID projectId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID issueTypeId = UUID.randomUUID();
        UUID priorityId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();

        Specification<Issue> spec = IssueSpecification.filter(
            projectId, statusId, assigneeId, issueTypeId, priorityId, sprintId, "bug");

        spec.toPredicate(root, query, cb);

        verify(cb, atLeastOnce()).equal(projectIdPath, projectId);
        verify(cb, atLeastOnce()).equal(statusIdPath, statusId);
        verify(cb, atLeastOnce()).equal(assigneeIdPath, assigneeId);
        verify(cb, atLeastOnce()).equal(issueTypeIdPath, issueTypeId);
        verify(cb, atLeastOnce()).equal(priorityIdPath, priorityId);
        verify(cb, atLeastOnce()).equal(sprintIdPath, sprintId);
        verify(cb).or(any(), any());
        verify(root).join("sprintIssues", JoinType.INNER);
    }

    @Test
    void filter_withNullParameters_skipsOptionalPredicates() {
        Specification<Issue> spec = IssueSpecification.filter(null, null, null, null, null, null, null);

        spec.toPredicate(root, query, cb);

        verify(root, never()).join(anyString(), any());
        verify(cb, never()).or(any(), any());
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void filter_withBlankKeyword_skipsKeywordPredicate() {
        Specification<Issue> spec = IssueSpecification.filter(UUID.randomUUID(), null, null, null, null, null, "   ");

        spec.toPredicate(root, query, cb);

        verify(cb, never()).or(any(), any());
    }
}
