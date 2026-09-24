package com.example.jari.issue.controller;

import com.example.jari.issue.dto.IssueResponse;
import com.example.jari.issue.service.GlobalSearchService;
import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.response.PageResponse;
import com.example.jari.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "Global search across workspaces and projects the user can access")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final GlobalSearchService globalSearchService;

    @Operation(
        summary = "Search issues globally",
        description = "Searches issue title/key across all projects the user can access "
            + "(project membership + workspace admin/owner). Uses Elasticsearch when available."
    )
    @GetMapping("/api/v1/search/issues")
    public ResponseEntity<ApiResponse<PageResponse<IssueResponse>>> searchIssues(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String term = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return ResponseEntity.ok(ApiResponse.ok(
            globalSearchService.searchIssues(user.getId(), term, page, size)));
    }
}
