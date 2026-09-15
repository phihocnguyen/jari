package com.example.jari.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.SerializationFeature;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // =========================================================================
    // Tạm thời chưa có Redis - Comment lại cấu hình RedisTemplate:
    // =========================================================================
    /*
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
        template.afterPropertiesSet();
        return template;
    }
    */

    /**
     * Cung cấp in-memory CacheManager thay thế cho Redis cache tạm thời,
     * đáp ứng yêu cầu bean CacheManager cho @EnableCaching và @Cacheable (ReferenceService).
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("ref:issue-types", "ref:statuses", "ref:priorities");
    }
}
