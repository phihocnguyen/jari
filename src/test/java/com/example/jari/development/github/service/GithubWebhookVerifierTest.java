package com.example.jari.development.github.service;

import com.example.jari.shared.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubWebhookVerifierTest {

    private GithubWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getGithub().setWebhookSecret("test-secret");
        props.getJwt().setSecret("jwt-fallback");
        verifier = new GithubWebhookVerifier(props);
    }

    @Test
    void roundTripsInstallState() {
        UUID workspaceId = UUID.randomUUID();
        String state = verifier.createInstallState(workspaceId);
        assertEquals(workspaceId, verifier.parseInstallState(state));
    }

    @Test
    void rejectsTamperedState() {
        UUID workspaceId = UUID.randomUUID();
        String state = verifier.createInstallState(workspaceId);
        String tampered = state.substring(0, state.length() - 2) + "xx";
        assertThrows(Exception.class, () -> verifier.parseInstallState(tampered));
    }

    @Test
    void verifiesValidSignature() throws Exception {
        byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        // Compute expected the same way the verifier does via creating a known signature
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hex = java.util.HexFormat.of().formatHex(mac.doFinal(body));
        verifier.verifySignature(body, "sha256=" + hex);
    }

    @Test
    void rejectsBadSignature() {
        byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> verifier.verifySignature(body, "sha256=deadbeef"));
    }
}
