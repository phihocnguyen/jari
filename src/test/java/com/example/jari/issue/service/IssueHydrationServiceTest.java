package com.example.jari.issue.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.sprint.entity.SprintIssue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueHydrationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Issue> issueQuery;

    @Mock
    private TypedQuery<SprintIssue> sprintIssueQuery;

    @InjectMocks
    private IssueHydrationService issueHydrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(issueHydrationService, "em", entityManager);
    }

    @Test
    void hydrateCollections_noOpForNullOrEmpty() {
        issueHydrationService.hydrateCollections((Collection<Issue>) null);
        issueHydrationService.hydrateCollections(Collections.emptyList());

        verifyNoInteractions(entityManager);
    }

    @Test
    void hydrateCollections_runsThreeBatchQueries() {
        UUID issueId = UUID.randomUUID();
        Issue issue = Issue.builder().id(issueId).build();

        when(entityManager.createQuery(anyString(), eq(Issue.class))).thenReturn(issueQuery);
        when(entityManager.createQuery(anyString(), eq(SprintIssue.class))).thenReturn(sprintIssueQuery);
        when(issueQuery.setParameter(eq("ids"), any())).thenReturn(issueQuery);
        when(sprintIssueQuery.setParameter(eq("ids"), any())).thenReturn(sprintIssueQuery);
        when(issueQuery.getResultList()).thenReturn(List.of(issue));
        when(sprintIssueQuery.getResultList()).thenReturn(List.of());

        issueHydrationService.hydrateCollections(List.of(issue));

        verify(entityManager, times(2)).createQuery(anyString(), eq(Issue.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(SprintIssue.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(issueQuery, times(2)).setParameter(eq("ids"), idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(issueId);
    }

    @Test
    void hydrateCollections_singleIssue_returnsSameInstance() {
        Issue issue = Issue.builder().id(UUID.randomUUID()).build();

        when(entityManager.createQuery(anyString(), eq(Issue.class))).thenReturn(issueQuery);
        when(entityManager.createQuery(anyString(), eq(SprintIssue.class))).thenReturn(sprintIssueQuery);
        when(issueQuery.setParameter(eq("ids"), any())).thenReturn(issueQuery);
        when(sprintIssueQuery.setParameter(eq("ids"), any())).thenReturn(sprintIssueQuery);
        when(issueQuery.getResultList()).thenReturn(List.of(issue));
        when(sprintIssueQuery.getResultList()).thenReturn(List.of());

        Issue result = issueHydrationService.hydrateCollections(issue);

        assertThat(result).isSameAs(issue);
    }
}
