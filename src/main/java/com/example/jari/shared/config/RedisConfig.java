package com.example.jari.shared.config;

import com.example.jari.shared.cache.CacheNames;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class RedisConfig {

    /**
     * Default typing (@class) bắt buộc để Redis cache đọc lại được DTO phức tạp (PageResponse, ProjectSummaryResponse, …).
     * Không truyền ObjectMapper thuần — serializer sẽ không ghi type hint và lần đọc cache thứ 2 trả 500.
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisJsonSerializer() {
        return new GenericJackson2JsonRedisSerializer()
            .configure(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            });
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisJsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisJsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory factory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer,
            CacheProperties cacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer));

        if (cacheProperties.getRedis().getTimeToLive() != null) {
            config = config.entryTtl(cacheProperties.getRedis().getTimeToLive());
        }
        if (!cacheProperties.getRedis().isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .enableStatistics()
            .initialCacheNames(Arrays.stream(cacheNames()).collect(Collectors.toSet()))
            .build();
    }

    /**
     * Cache names used by {@link org.springframework.cache.annotation.Cacheable} — TTL via spring.cache.redis.time-to-live.
     */
    public static String[] cacheNames() {
        return new String[] {
            CacheNames.REF_ISSUE_TYPES,
            CacheNames.REF_STATUSES,
            CacheNames.REF_PRIORITIES,
            CacheNames.PROJECT_SUMMARY,
            CacheNames.PROJECT_BOARD,
            CacheNames.ISSUE_DETAIL,
            CacheNames.ISSUE_LIST,
        };
    }
}
