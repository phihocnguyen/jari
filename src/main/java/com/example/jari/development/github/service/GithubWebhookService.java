package com.example.jari.development.github.service;

import com.example.jari.development.github.repository.GithubInstallationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookService {

    private final GithubInstallationService installationService;
    private final DevelopmentLinkService developmentLinkService;
    private final GithubInstallationRepository installationRepository;

    @Transactional
    public void handle(String event, JsonNode payload) {
        if (event == null) {
            return;
        }
        switch (event) {
            case "installation" -> handleInstallation(payload);
            case "installation_repositories" -> handleInstallationRepositories(payload);
            case "pull_request" -> handlePullRequest(payload);
            case "push" -> handlePush(payload);
            case "create" -> handleCreate(payload);
            default -> log.debug("Ignoring GitHub event: {}", event);
        }
    }

    private void handleInstallation(JsonNode payload) {
        String action = payload.path("action").asText();
        long installationId = payload.path("installation").path("id").asLong();
        JsonNode account = payload.path("installation").path("account");

        if ("deleted".equals(action) || "suspend".equals(action)) {
            installationService.removeInstallation(installationId);
            return;
        }

        if ("created".equals(action) || "unsuspend".equals(action) || "new_permissions_accepted".equals(action)) {
            // Setup callback with state usually links workspace; webhook may arrive first.
            // If we already have the installation linked, sync repos from payload.
            JsonNode repos = payload.path("repositories");
            installationRepository.findByInstallationId(installationId).ifPresentOrElse(
                inst -> installationService.upsertInstallationFromWebhook(
                    installationId, inst.getWorkspace().getId(), account, repos.isMissingNode() ? null : repos),
                () -> log.info("installation.created {} — awaiting setup callback with state", installationId)
            );
        }
    }

    private void handleInstallationRepositories(JsonNode payload) {
        long installationId = payload.path("installation").path("id").asLong();
        String action = payload.path("action").asText();
        if ("added".equals(action)) {
            installationService.addReposFromWebhook(installationId, payload.path("repositories_added"));
        } else if ("removed".equals(action)) {
            installationService.removeReposFromWebhook(installationId, payload.path("repositories_removed"));
        }
    }

    private void handlePullRequest(JsonNode payload) {
        String action = payload.path("action").asText();
        if (!isPrActionRelevant(action)) {
            return;
        }
        JsonNode pr = payload.path("pull_request");
        JsonNode repo = payload.path("repository");
        long githubRepoId = repo.path("id").asLong();
        String fullName = repo.path("full_name").asText();
        String htmlUrl = repo.path("html_url").asText("https://github.com/" + fullName);
        int number = pr.path("number").asInt();
        String title = pr.path("title").asText("");
        String prUrl = pr.path("html_url").asText();
        String headRef = pr.path("head").path("ref").asText("");
        String author = pr.path("user").path("login").asText(null);
        boolean merged = pr.path("merged").asBoolean(false);
        String state = pr.path("state").asText("open");
        String status = merged ? "MERGED" : ("closed".equalsIgnoreCase(state) ? "CLOSED" : "OPEN");

        developmentLinkService.linkFromRepo(
            githubRepoId,
            "PULL_REQUEST",
            String.valueOf(number),
            title.isBlank() ? ("PR #" + number) : title,
            prUrl,
            htmlUrl,
            status,
            author,
            title,
            headRef
        );
    }

    private void handlePush(JsonNode payload) {
        JsonNode repo = payload.path("repository");
        long githubRepoId = repo.path("id").asLong();
        String fullName = repo.path("full_name").asText();
        String htmlUrl = repo.path("html_url").asText("https://github.com/" + fullName);
        JsonNode commits = payload.path("commits");
        if (!commits.isArray()) {
            return;
        }
        // Also try branch name from ref: refs/heads/feature/APP-1-x
        String ref = payload.path("ref").asText("");
        String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;

        for (JsonNode commit : commits) {
            String sha = commit.path("id").asText();
            String message = commit.path("message").asText("");
            String firstLine = message.lines().findFirst().orElse(message);
            String commitUrl = commit.path("url").asText();
            // GitHub webhook commit.url is API URL; prefer html via repo
            if (commitUrl != null && commitUrl.contains("api.github.com")) {
                commitUrl = htmlUrl + "/commit/" + sha;
            }
            String author = commit.path("author").path("username").asText(null);
            if (author == null || author.isBlank()) {
                author = commit.path("author").path("name").asText(null);
            }

            developmentLinkService.linkFromRepo(
                githubRepoId,
                "COMMIT",
                sha,
                firstLine.isBlank() ? sha.substring(0, Math.min(7, sha.length())) : firstLine,
                commitUrl,
                htmlUrl,
                null,
                author,
                message,
                branch
            );
        }
    }

    private void handleCreate(JsonNode payload) {
        String refType = payload.path("ref_type").asText();
        if (!"branch".equals(refType)) {
            return;
        }
        JsonNode repo = payload.path("repository");
        long githubRepoId = repo.path("id").asLong();
        String fullName = repo.path("full_name").asText();
        String htmlUrl = repo.path("html_url").asText("https://github.com/" + fullName);
        String ref = payload.path("ref").asText();
        String branchUrl = htmlUrl + "/tree/" + ref;
        String sender = payload.path("sender").path("login").asText(null);

        developmentLinkService.linkFromRepo(
            githubRepoId,
            "BRANCH",
            ref,
            ref,
            branchUrl,
            htmlUrl,
            "OPEN",
            sender,
            ref
        );
    }

    private static boolean isPrActionRelevant(String action) {
        return switch (action) {
            case "opened", "edited", "reopened", "synchronize", "closed", "ready_for_review" -> true;
            default -> false;
        };
    }
}
