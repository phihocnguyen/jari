package com.example.jari.development.github.service;

import com.example.jari.shared.config.AppProperties;
import com.example.jari.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubApiClient {

    private final GithubAppJwtService jwtService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public String createInstallationToken(long installationId) {
        String jwt = jwtService.createAppJwt();
        RestClient client = restClientBuilder.build();
        String body = client.post()
            .uri("https://api.github.com/app/installations/{id}/access_tokens", installationId)
            .header("Authorization", "Bearer " + jwt)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(body);
            String token = node.path("token").asText(null);
            if (token == null || token.isBlank()) {
                throw new BadRequestException("GitHub installation token missing in response");
            }
            return token;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse GitHub installation token: " + e.getMessage());
        }
    }

    public JsonNode getInstallation(long installationId) {
        String jwt = jwtService.createAppJwt();
        RestClient client = restClientBuilder.build();
        String body = client.get()
            .uri("https://api.github.com/app/installations/{id}", installationId)
            .header("Authorization", "Bearer " + jwt)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .retrieve()
            .body(String.class);
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new BadRequestException("Failed to fetch GitHub installation: " + e.getMessage());
        }
    }

    public List<RepoInfo> listInstallationRepos(long installationId) {
        String token = createInstallationToken(installationId);
        RestClient client = restClientBuilder.build();
        List<RepoInfo> repos = new ArrayList<>();
        int page = 1;
        while (page <= 10) {
            final int currentPage = page;
            String body = client.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("api.github.com")
                    .path("/installation/repositories")
                    .queryParam("per_page", 100)
                    .queryParam("page", currentPage)
                    .build())
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(String.class);
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode list = root.path("repositories");
                if (!list.isArray() || list.isEmpty()) {
                    break;
                }
                for (JsonNode r : list) {
                    repos.add(new RepoInfo(
                        r.path("id").asLong(),
                        r.path("full_name").asText(),
                        r.path("html_url").asText(null)
                    ));
                }
                if (list.size() < 100) {
                    break;
                }
                page++;
            } catch (Exception e) {
                throw new BadRequestException("Failed to list GitHub repos: " + e.getMessage());
            }
        }
        return repos;
    }

    public record RepoInfo(long id, String fullName, String htmlUrl) {}

    public boolean isConfigured() {
        AppProperties.Github gh = appProperties.getGithub();
        return gh.getAppId() != null && !gh.getAppId().isBlank()
            && gh.getAppSlug() != null && !gh.getAppSlug().isBlank()
            && gh.getPrivateKeyPem() != null && !gh.getPrivateKeyPem().isBlank();
    }
}
