package com.example.jari.issue.search;

import com.example.jari.issue.dto.IssueFilterRequest;
import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.service.IssueHydrationService;
import com.example.jari.shared.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueSearchServiceTest {

    @Mock private ElasticsearchOperations operations;
    @Mock private IssueRepository issueRepository;
    @Mock private IssueMapper mapper;
    @Mock private IssueHydrationService issueHydrationService;
    @Mock private SearchHits<IssueSearchDocument> searchHits;
    @Mock private SearchHit<IssueSearchDocument> searchHit;
    @InjectMocks private IssueSearchService issueSearchService;

    @Test
    void search_returnsNullWhenElasticsearchFails() {
        UUID projectId = UUID.randomUUID();
        IssueFilterRequest filter = new IssueFilterRequest();
        when(operations.search(any(Query.class), eq(IssueSearchDocument.class)))
            .thenThrow(new RuntimeException("ES down"));

        PageResponse<IssueResponse> result = issueSearchService.search(projectId, filter);

        assertThat(result).isNull();
        verify(issueRepository, never()).findAllWithDetailsByIdIn(any());
    }

    @Test
    void search_appliesAllFilters() {
        UUID projectId = UUID.randomUUID();
        IssueFilterRequest filter = new IssueFilterRequest();
        filter.setStatusId(UUID.randomUUID());
        filter.setAssigneeId(UUID.randomUUID());
        filter.setIssueTypeId(UUID.randomUUID());
        filter.setPriorityId(UUID.randomUUID());
        filter.setSprintId(UUID.randomUUID());
        filter.setKeyword("bug*fix");

        when(operations.search(any(Query.class), eq(IssueSearchDocument.class))).thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        PageResponse<IssueResponse> result = issueSearchService.search(projectId, filter);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void search_hydratesIssuesAndMapsResponses() {
        UUID projectId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();
        IssueFilterRequest filter = new IssueFilterRequest();

        IssueSearchDocument document = new IssueSearchDocument();
        document.setId(issueId.toString());

        Issue issue = Issue.builder().id(issueId).build();
        IssueResponse response = IssueResponse.builder().id(issueId).title("Fix board").build();

        when(operations.search(any(Query.class), eq(IssueSearchDocument.class))).thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHit.getContent()).thenReturn(document);
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(issueRepository.findAllWithDetailsByIdIn(List.of(issueId))).thenReturn(List.of(issue));
        when(mapper.toResponse(issue)).thenReturn(response);

        PageResponse<IssueResponse> result = issueSearchService.search(projectId, filter);

        assertThat(result).isNotNull();
        assertThat(result.getData()).containsExactly(response);
        assertThat(result.getTotal()).isEqualTo(1);
        verify(issueHydrationService).hydrateCollections(org.mockito.ArgumentMatchers.<Collection<Issue>>argThat(collection ->
            collection != null && collection.size() == 1 && collection.iterator().next().equals(issue)));
    }
}
