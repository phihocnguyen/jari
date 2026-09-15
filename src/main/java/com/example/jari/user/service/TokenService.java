package com.example.jari.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// import org.springframework.data.redis.core.RedisTemplate;
// import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    // =========================================================================
    // Tạm thời chưa có Redis, comment lại:
    // private final RedisTemplate<String, Object> redisTemplate;
    // =========================================================================

    // Fallback lưu trữ in-memory khi chưa có Redis:
    private final Map<String, String> inMemoryTokens = new ConcurrentHashMap<>();
    private final Set<String> inMemoryBlacklist = ConcurrentHashMap.newKeySet();

    private static final String REFRESH_KEY_PREFIX  = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    public void saveRefreshToken(UUID userId, String refreshToken, long expirySeconds) {
        // redisTemplate.opsForValue()
        //     .set(REFRESH_KEY_PREFIX + userId, refreshToken, Duration.ofSeconds(expirySeconds));
        inMemoryTokens.put(REFRESH_KEY_PREFIX + userId, refreshToken);
    }

    public boolean isRefreshTokenValid(UUID userId, String refreshToken) {
        // Object stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        // return refreshToken.equals(stored);
        String stored = inMemoryTokens.get(REFRESH_KEY_PREFIX + userId);
        return refreshToken.equals(stored);
    }

    public void deleteRefreshToken(UUID userId) {
        // redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        inMemoryTokens.remove(REFRESH_KEY_PREFIX + userId);
    }

    public void blacklistAccessToken(String token, long remainingSeconds) {
        // if (remainingSeconds > 0) {
        //     redisTemplate.opsForValue()
        //         .set(BLACKLIST_KEY_PREFIX + token, "1", Duration.ofSeconds(remainingSeconds));
        // }
        if (remainingSeconds > 0) {
            inMemoryBlacklist.add(BLACKLIST_KEY_PREFIX + token);
        }
    }

    public boolean isAccessTokenBlacklisted(String token) {
        // return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
        return inMemoryBlacklist.contains(BLACKLIST_KEY_PREFIX + token);
    }
}
