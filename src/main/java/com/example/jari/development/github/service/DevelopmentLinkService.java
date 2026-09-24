package com.example.jari.development.github.service;

import com.example.jari.development.entity.IssueDevelopment;
import com.example.jari.development.repository.IssueDevelopmentRepository;
import com.example.jari.development.github.entity.GithubRepo;
import com.example.jari.development.github.repository.GithubRepoRepository;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevelopmentLinkService {

    private final IssueKeyParser issueKeyParser;
    private final IssueRepository issueRepository;
    private final IssueDevelopmentRepository developmentRepository;
    private final GithubRepoRepository githubRepoRepository;

    @Transactional
    public void linkFromRepo(
        long githubRepoId,
        String type,
        String externalId,
        String title,
        String url,
        String repoUrl,
        String status,
        String author,
        String... textsWithKeys
    ) {
        GithubRepo repo = githubRepoRepository.findByGithubRepoIdWithInstallation(githubRepoId).orElse(null);
        if (repo == null) {
            log.debug("Ignoring GitHub event for unknown repo id={}", githubRepoId);
            return;
        }
        if (repo.getProject() == null) {
            log.debug("Repo {} is not mapped to a Jari project — skip linking", repo.getFullName());
            return;
        }

        UUID projectId = repo.getProject().getId();
        List<String> keys = issueKeyParser.extractKeys(textsWithKeys);
        if (keys.isEmpty()) {
            return;
        }

        String normalizedType = type.toUpperCase().trim();
        for (String key : keys) {
            issueRepository.findByIssueKeyIgnoreCase(key).ifPresent(issue -> {
                if (!issue.getProject().getId().equals(projectId)) {
                    log.debug("Issue {} belongs to another project — skip", key);
                    return;
                }
                upsert(issue, normalizedType, externalId, githubRepoId, title, url, repoUrl, status, author);
            });
        }
    }

    private void upsert(
        Issue issue,
        String type,
        String externalId,
        long githubRepoId,
        String title,
        String url,
        String repoUrl,
        String status,
        String author
    ) {
        IssueDevelopment existing = developmentRepository
            .findByIssueIdAndTypeAndExternalIdAndGithubRepoId(issue.getId(), type, externalId, githubRepoId)
            .orElse(null);

        if (existing != null) {
            existing.setTitle(truncate(title, 255));
            existing.setUrl(truncate(url, 500));
            if (repoUrl != null) {
                existing.setRepoUrl(truncate(repoUrl, 500));
            }
            if (status != null) {
                existing.setStatus(status.toUpperCase());
            }
            if (author != null) {
                existing.setAuthor(truncate(author, 100));
            }
            developmentRepository.save(existing);
            return;
        }

        IssueDevelopment created = IssueDevelopment.builder()
            .issue(issue)
            .type(type)
            .externalId(externalId)
            .githubRepoId(githubRepoId)
            .title(truncate(title, 255))
            .url(truncate(url, 500))
            .repoUrl(repoUrl != null ? truncate(repoUrl, 500) : null)
            .status(status != null ? status.toUpperCase() : null)
            .author(author != null ? truncate(author, 100) : null)
            .build();
        developmentRepository.save(created);
        log.info("Linked {} {} to issue {}", type, externalId, issue.getIssueKey());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
