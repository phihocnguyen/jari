package com.example.jari.shared.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
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
        // Do not clear entire ISSUE_LIST — that nukes every project's list cache and
        // forces expensive rebuilds under write-heavy load. List entries expire via TTL.
    }

    /**
     * Evict issue detail always; summary always; board only when {@code evictBoard} is true.
     * Callers should pass {@code false} when the write cannot change the active-sprint board
     * (e.g. backlog-only create/update) to avoid the write→evict→miss→full board rebuild loop.
     */
    public void evictIssue(UUID issueId, String issueKey, UUID projectId, boolean evictBoard) {
        if (issueId != null) {
            evictKey(CacheNames.ISSUE_DETAIL, issueId);
        }
        if (issueKey != null) {
            evictKey(CacheNames.ISSUE_DETAIL, issueKey.toUpperCase());
            evictKey(CacheNames.ISSUE_DETAIL, issueKey);
        }
        if (projectId != null) {
            evictKey(CacheNames.PROJECT_SUMMARY, projectId);
            if (evictBoard) {
                evictBoard(projectId);
            }
        }
    }

    /** Convenience: evict detail + summary + board (safe default). */
    public void evictIssue(UUID issueId, String issueKey, UUID projectId) {
        evictIssue(issueId, issueKey, projectId, true);
    }

    private void evictKey(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        } catch (RuntimeException ex) {
            // Never fail the business TX because Redis is slow/unavailable under load
            log.warn("Cache evict failed for {} key={}: {}", cacheName, key, ex.getMessage());
        }
    }
}
