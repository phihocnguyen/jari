package com.example.jari.shared.cache;

import com.example.jari.issue.dto.IssueFilterRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IssueFilterCacheKeyTest {

    @Test
    void of_includesProjectAndPagination() {
        UUID projectId = UUID.randomUUID();
        IssueFilterRequest filter = new IssueFilterRequest();
        filter.setPage(1);
        filter.setSize(50);
        filter.setKeyword(" Bug ");

        String key = IssueFilterCacheKey.of(projectId, filter);

        assertThat(key).startsWith(projectId + ":1:50:");
    }

    @Test
    void of_normalizesKeywordCase() {
        UUID projectId = UUID.randomUUID();
        IssueFilterRequest a = new IssueFilterRequest();
        a.setKeyword("BUG");
        IssueFilterRequest b = new IssueFilterRequest();
        b.setKeyword(" bug ");

        assertThat(IssueFilterCacheKey.of(projectId, a)).isEqualTo(IssueFilterCacheKey.of(projectId, b));
    }
}
