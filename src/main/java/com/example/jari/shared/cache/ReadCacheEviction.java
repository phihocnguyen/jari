package com.example.jari.shared.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadCacheEviction {

    private final CacheManager cacheManager;

    public void evictBoard(UUID projectId) {
        if (projectId != null) {
            evictKey(CacheNames.PROJECT_BOARD, projectId);
        }
    }

    public void evictProject(UUID projectId) {
        if (projectId == null) {
            return;
        }
        evictKey(CacheNames.PROJECT_SUMMARY, projectId);
        evictBoard(projectId);
        clearCache(CacheNames.ISSUE_LIST);
    }

    public void evictIssue(UUID issueId, String issueKey, UUID projectId) {
        if (issueId != null) {
            evictKey(CacheNames.ISSUE_DETAIL, issueId);
        }
        if (issueKey != null) {
            evictKey(CacheNames.ISSUE_DETAIL, issueKey.toUpperCase());
            evictKey(CacheNames.ISSUE_DETAIL, issueKey);
        }
        evictProject(projectId);
    }

    private void evictKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
