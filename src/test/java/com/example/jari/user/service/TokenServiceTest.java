package com.example.jari.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void saveRefreshToken_storesWithExpiry() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        tokenService.saveRefreshToken(userId, "token", 3600);

        verify(valueOps).set(eq("refresh:" + userId), eq("token"), eq(Duration.ofSeconds(3600)));
    }

    @Test
    void isRefreshTokenValid_comparesStoredValue() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh:" + userId)).thenReturn("token");

        assertThat(tokenService.isRefreshTokenValid(userId, "token")).isTrue();
        assertThat(tokenService.isRefreshTokenValid(userId, "other")).isFalse();
    }

    @Test
    void blacklistAccessToken_skipsWhenExpiryZero() {
        tokenService.blacklistAccessToken("token", 0);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklistAccessToken_storesWhenExpiryPositive() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        tokenService.blacklistAccessToken("token", 60);

        verify(valueOps).set(eq("blacklist:token"), eq("1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void isAccessTokenBlacklisted_delegatesToRedis() {
        when(redisTemplate.hasKey("blacklist:token")).thenReturn(true);

        assertThat(tokenService.isAccessTokenBlacklisted("token")).isTrue();
    }
}
