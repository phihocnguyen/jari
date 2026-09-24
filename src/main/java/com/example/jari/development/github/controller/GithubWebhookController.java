package com.example.jari.development.github.controller;

import com.example.jari.development.github.service.GithubWebhookService;
import com.example.jari.development.github.service.GithubWebhookVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookController {

    private final GithubWebhookVerifier verifier;
    private final GithubWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/github")
    public ResponseEntity<Void> receive(
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        @RequestHeader(value = "X-GitHub-Event", required = false) String event,
        @RequestHeader(value = "X-GitHub-Delivery", required = false) String delivery,
        @RequestBody byte[] rawBody
    ) throws Exception {
        verifier.verifySignature(rawBody, signature);
        JsonNode payload = objectMapper.readTree(rawBody);
        log.info("GitHub webhook event={} delivery={}", event, delivery);
        webhookService.handle(event, payload);
        return ResponseEntity.ok().build();
    }
}
