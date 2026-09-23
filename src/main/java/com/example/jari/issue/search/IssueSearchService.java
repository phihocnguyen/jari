package com.example.jari.issue.search;

import com.example.jari.issue.dto.IssueFilterRequest;
import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.service.IssueHydrationService;
import com.example.jari.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Truy vấn danh sách issue từ Elasticsearch (index "jari-issues" denormalized)
 * thay vì chạy Specification join nhiều bảng trên PostgreSQL. Sau khi ES filter,
 * chỉ hydrate entity theo ID (primary-key lookup) để có đủ labels/components/...
 * cho IssueResponse.
 *
 * ES có thể chưa sẵn sàng (chưa deploy monitoring stack, dữ liệu chưa sync):
 * {@link #search} trả về null để caller fallback về JPA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueSearchService {

    private final ElasticsearchOperations operations;
    private final IssueRepository issueRepository;
    private final IssueMapper mapper;
    private final IssueHydrationService issueHydrationService;

    /**
     * @return kết quả search, hoặc null nếu ES lỗi/không khả dụng (caller fallback DB)
     */
    public PageResponse<IssueResponse> search(UUID projectId, IssueFilterRequest filter) {
        try {
            Criteria criteria = buildCriteria(projectId, filter);

            Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.ASC, "position").and(Sort.by(Sort.Direction.DESC, "createdAt")));
            CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);

            SearchHits<IssueSearchDocument> hits = operations.search(query, IssueSearchDocument.class);

            // Giữ nguyên thứ tự sắp xếp của ES
            List<UUID> ids = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(IssueSearchDocument::getId)
                .map(UUID::fromString)
                .toList();

            Map<UUID, Issue> byId = issueRepository.findAllWithDetailsByIdIn(ids).stream()
                .collect(Collectors.toMap(Issue::getId, Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));

            issueHydrationService.hydrateCollections(byId.values());

            List<IssueResponse> content = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(mapper::toResponse)
                .toList();

            PageImpl<IssueResponse> page = new PageImpl<>(content, pageable, hits.getTotalHits());
            return PageResponse.of(page);
        } catch (Exception e) {
            log.warn("Elasticsearch search unavailable, falling back to JPA: {}", e.getMessage());
            return null;
        }
    }

    private Criteria buildCriteria(UUID projectId, IssueFilterRequest filter) {
        Criteria criteria = new Criteria();

        if (projectId != null) {
            criteria = criteria.and(new Criteria("projectId").is(projectId.toString()));
        }
        if (filter.getStatusId() != null) {
            criteria = criteria.and(new Criteria("statusId").is(filter.getStatusId().toString()));
        }
        if (filter.getAssigneeId() != null) {
            criteria = criteria.and(new Criteria("assigneeId").is(filter.getAssigneeId().toString()));
        }
        if (filter.getIssueTypeId() != null) {
            criteria = criteria.and(new Criteria("issueTypeId").is(filter.getIssueTypeId().toString()));
        }
        if (filter.getPriorityId() != null) {
            criteria = criteria.and(new Criteria("priorityId").is(filter.getPriorityId().toString()));
        }
        if (filter.getSprintId() != null) {
            criteria = criteria.and(new Criteria("sprintIds").is(filter.getSprintId().toString()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            // Ngữ nghĩa như cũ: LIKE '%kw%' không phân biệt hoa thường trên title/issueKey.
            // title_lower/issue_key_lower là field keyword lowercase do indexer sinh sẵn.
            criteria = criteria.and(new Criteria("titleLower")
                .expression(containsWildcard(filter.getKeyword()))
                .or(new Criteria("issueKeyLower").expression(containsWildcard(filter.getKeyword()))));
        }
        return criteria;
    }

    private String containsWildcard(String keyword) {
        String escaped = keyword.toLowerCase().trim()
            .replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("?", "\\?");
        return "*" + escaped + "*";
    }
}
