package com.example.jari.development.github.service;

import com.example.jari.shared.config.AppProperties;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubWebhookVerifier {

    private final AppProperties appProperties;

    public void verifySignature(byte[] payload, String signatureHeader) {
        String secret = appProperties.getGithub().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new BadRequestException("GitHub webhook secret is not configured");
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new ForbiddenException("Missing or invalid X-Hub-Signature-256");
        }
        String expected = "sha256=" + hmacSha256Hex(secret, payload);
        if (!constantTimeEquals(expected, signatureHeader)) {
            throw new ForbiddenException("Invalid GitHub webhook signature");
        }
    }

    /** Signed state: workspaceId.timestamp.hmac */
    public String createInstallState(UUID workspaceId) {
        long ts = System.currentTimeMillis();
        String payload = workspaceId + "." + ts;
        String sig = hmacSha256Base64(signingKey(), payload);
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((payload + "." + sig).getBytes(StandardCharsets.UTF_8));
    }

    public UUID parseInstallState(String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("Missing install state");
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.", 3);
            if (parts.length != 3) {
                throw new BadRequestException("Invalid install state");
            }
            UUID workspaceId = UUID.fromString(parts[0]);
            long ts = Long.parseLong(parts[1]);
            String sig = parts[2];
            // 1 hour validity
            if (Math.abs(System.currentTimeMillis() - ts) > 3_600_000L) {
                throw new BadRequestException("Install state expired");
            }
            String expected = hmacSha256Base64(signingKey(), parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expected, sig)) {
                throw new ForbiddenException("Invalid install state signature");
            }
            return workspaceId;
        } catch (BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid install state");
        }
    }

    private String signingKey() {
        String secret = appProperties.getGithub().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            secret = appProperties.getJwt().getSecret();
        }
        if (secret == null || secret.isBlank()) {
            throw new BadRequestException("No secret available to sign GitHub install state");
        }
        return secret;
    }

    private static String hmacSha256Hex(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static String hmacSha256Base64(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
