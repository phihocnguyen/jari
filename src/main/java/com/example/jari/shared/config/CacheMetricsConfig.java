package com.example.jari.shared.config;

import com.example.jari.shared.cache.CacheNames;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Redis caches are created lazily on first {@code @Cacheable} use — bind each to Micrometer
 * so Prometheus exposes {@code cache_gets_total{result="hit|miss"}}.
 */
@Component
public class CacheMetricsConfig {

    private final CacheMetricsRegistrar cacheMetricsRegistrar;
    private final CacheManager cacheManager;

    public CacheMetricsConfig(CacheMetricsRegistrar cacheMetricsRegistrar, CacheManager cacheManager) {
        this.cacheMetricsRegistrar = cacheMetricsRegistrar;
        this.cacheManager = cacheManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bindCacheMetrics() {
        for (String cacheName : RedisConfig.cacheNames()) {
            bindIfPresent(cacheManager.getCache(cacheName));
        }
    }

    private void bindIfPresent(Cache cache) {
        if (cache != null) {
            cacheMetricsRegistrar.bindCacheToRegistry(cache);
        }
    }
}
