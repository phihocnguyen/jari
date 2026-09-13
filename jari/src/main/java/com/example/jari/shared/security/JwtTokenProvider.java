package com.example.jari.shared.security;

import com.example.jari.shared.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String email) {
        long expiryMs = appProperties.getJwt().getAccessTokenExpiry() * 1000L;
        return buildToken(userId.toString(), email, expiryMs);
    }

    public String generateRefreshToken(UUID userId) {
        long expiryMs = appProperties.getJwt().getRefreshTokenExpiry() * 1000L;
        return buildToken(userId.toString(), null, expiryMs);
    }

    private String buildToken(String subject, String email, long expiryMs) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiryMs))
            .signWith(getSigningKey());

        if (email != null) {
            builder.claim("email", email);
        }
        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirySeconds() {
        return appProperties.getJwt().getAccessTokenExpiry();
    }

    public long getRefreshTokenExpirySeconds() {
        return appProperties.getJwt().getRefreshTokenExpiry();
    }
}
