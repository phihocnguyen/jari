package com.example.jari.shared.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadCacheEvictionTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache issueDetailCache;

    @Mock
    private Cache issueListCache;

    @Mock
    private Cache boardCache;

    @Mock
    private Cache summaryCache;

    @InjectMocks
    private ReadCacheEviction readCacheEviction;

    @Test
    void evictIssue_evictsDetailKeysAndProjectCaches() {
        UUID issueId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String issueKey = "tis-1";

        when(cacheManager.getCache(CacheNames.ISSUE_DETAIL)).thenReturn(issueDetailCache);
        when(cacheManager.getCache(CacheNames.ISSUE_LIST)).thenReturn(issueListCache);
        when(cacheManager.getCache(CacheNames.PROJECT_BOARD)).thenReturn(boardCache);
        when(cacheManager.getCache(CacheNames.PROJECT_SUMMARY)).thenReturn(summaryCache);

        readCacheEviction.evictIssue(issueId, issueKey, projectId);

        verify(issueDetailCache).evict(issueId);
        verify(issueDetailCache).evict("TIS-1");
        verify(issueDetailCache).evict("tis-1");
        verify(summaryCache).evict(projectId);
        verify(boardCache).evict(projectId);
        verify(issueListCache).clear();
    }

    @Test
    void evictBoard_skipsWhenProjectIdNull() {
        readCacheEviction.evictBoard(null);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void evictProject_skipsWhenProjectIdNull() {
        readCacheEviction.evictProject(null);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void evictProject_clearsIssueListAndBoard() {
        UUID projectId = UUID.randomUUID();
        when(cacheManager.getCache(CacheNames.PROJECT_SUMMARY)).thenReturn(summaryCache);
        when(cacheManager.getCache(CacheNames.PROJECT_BOARD)).thenReturn(boardCache);
        when(cacheManager.getCache(CacheNames.ISSUE_LIST)).thenReturn(issueListCache);

        readCacheEviction.evictProject(projectId);

        verify(summaryCache).evict(projectId);
        verify(boardCache).evict(projectId);
        verify(issueListCache).clear();
    }
}
