package com.example.jari.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    public void saveRefreshToken(UUID userId, String refreshToken, long expirySeconds) {
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + userId,
            refreshToken,
            Duration.ofSeconds(expirySeconds));
    }

    public boolean isRefreshTokenValid(UUID userId, String refreshToken) {
        Object stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        return refreshToken.equals(stored);
    }

    public void deleteRefreshToken(UUID userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    public void blacklistAccessToken(String token, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + token,
                "1",
                Duration.ofSeconds(remainingSeconds));
        }
    }

    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }
}
