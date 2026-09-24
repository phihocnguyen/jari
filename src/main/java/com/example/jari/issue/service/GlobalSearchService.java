package com.example.jari.issue.service;

import com.example.jari.issue.dto.IssueFilterRequest;
import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.issue.search.IssueSearchService;
import com.example.jari.issue.spec.IssueSpecification;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-workspace issue search for the global ⌘K palette (Jira-style work items).
 */
@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final IssueSearchService issueSearchService;
    private final IssueRepository issueRepository;
    private final IssueMapper mapper;
    private final IssueHydrationService issueHydrationService;

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> searchIssues(UUID userId, String keyword, int page, int size) {
        Set<UUID> projectIds = resolveAccessibleProjectIds(userId);
        if (projectIds.isEmpty()) {
            return PageResponse.of(org.springframework.data.domain.Page.empty(PageRequest.of(page, size)));
        }

        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        IssueFilterRequest filter = new IssueFilterRequest();
        filter.setKeyword(keyword != null ? keyword.trim() : "");
        filter.setPage(safePage);
        filter.setSize(safeSize);

        PageResponse<IssueResponse> esResult = issueSearchService.searchAcross(projectIds, filter);
        if (esResult != null) {
            return esResult;
        }

        var spec = IssueSpecification.filter(
            null,
            projectIds,
            null, null, null, null, null,
            filter.getKeyword());

        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        var result = issueRepository.findAll(spec, pageable);
        issueHydrationService.hydrateCollections(result.getContent());
        return PageResponse.of(result.map(mapper::toResponse));
    }

    private Set<UUID> resolveAccessibleProjectIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        ids.addAll(projectMemberRepository.findProjectIdsByUserId(userId));
        ids.addAll(projectRepository.findProjectIdsVisibleAsWorkspaceAdmin(userId));
        return ids;
    }
}
