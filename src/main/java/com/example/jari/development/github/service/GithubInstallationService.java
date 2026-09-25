package com.example.jari.development.github.service;

import com.example.jari.development.github.entity.GithubInstallation;
import com.example.jari.development.github.entity.GithubRepo;
import com.example.jari.development.github.repository.GithubInstallationRepository;
import com.example.jari.development.github.repository.GithubRepoRepository;
import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.shared.config.AppProperties;
import com.example.jari.shared.exception.BadRequestException;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import com.example.jari.workspace.repository.WorkspaceMemberRepository;
import com.example.jari.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubInstallationService {

    private final AppProperties appProperties;
    private final GithubWebhookVerifier stateSigner;
    private final GithubApiClient githubApiClient;
    private final GithubInstallationRepository installationRepository;
    private final GithubRepoRepository repoRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public String buildInstallUrl(UUID userId, UUID workspaceId) {
        requireAdmin(userId, workspaceId);
        AppProperties.Github gh = appProperties.getGithub();
        if (!githubApiClient.isConfigured()) {
            throw new BadRequestException(
                "GitHub App is not configured. Set GITHUB_APP_ID, GITHUB_APP_SLUG, and GITHUB_APP_PRIVATE_KEY.");
        }
        String state = stateSigner.createInstallState(workspaceId);
        return UriComponentsBuilder
            .fromUriString("https://github.com/apps/" + gh.getAppSlug() + "/installations/new")
            .queryParam("state", state)
            .build()
            .toUriString();
    }

    @Transactional
    public UUID linkInstallationFromSetup(String state, long installationId, JsonNode account) {
        UUID workspaceId = stateSigner.parseInstallState(state);
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        String login = account != null ? account.path("login").asText("unknown") : "unknown";
        String type = account != null ? account.path("type").asText(null) : null;

        GithubInstallation installation = installationRepository.findByInstallationId(installationId)
            .orElse(null);

        if (installation == null) {
            installation = GithubInstallation.builder()
                .workspace(workspace)
                .installationId(installationId)
                .accountLogin(login)
                .accountType(type)
                .build();
        } else {
            installation.setWorkspace(workspace);
            installation.setAccountLogin(login);
            installation.setAccountType(type);
        }
        installation = installationRepository.save(installation);
        syncReposFromApi(installation);
        return workspaceId;
    }

    @Transactional
    public void upsertInstallationFromWebhook(long installationId, UUID preferredWorkspaceId, JsonNode account, JsonNode repositories) {
        GithubInstallation installation = installationRepository.findByInstallationId(installationId).orElse(null);
        if (installation == null) {
            if (preferredWorkspaceId == null) {
                log.warn("installation webhook for {} with no linked workspace — waiting for setup callback", installationId);
                return;
            }
            Workspace workspace = workspaceRepository.findById(preferredWorkspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", preferredWorkspaceId));
            installation = GithubInstallation.builder()
                .workspace(workspace)
                .installationId(installationId)
                .accountLogin(account.path("login").asText("unknown"))
                .accountType(account.path("type").asText(null))
                .build();
            installation = installationRepository.save(installation);
        } else {
            installation.setAccountLogin(account.path("login").asText(installation.getAccountLogin()));
            installation.setAccountType(account.path("type").asText(installation.getAccountType()));
            installation = installationRepository.save(installation);
        }

        if (repositories != null && repositories.isArray()) {
            syncReposFromPayload(installation, repositories);
        } else {
            syncReposFromApi(installation);
        }
    }

    @Transactional
    public void removeInstallation(long installationId) {
        installationRepository.findByInstallationId(installationId).ifPresent(inst -> {
            installationRepository.delete(inst);
            log.info("Removed GitHub installation {}", installationId);
        });
    }

    @Transactional
    public void addReposFromWebhook(long installationId, JsonNode repositoriesAdded) {
        GithubInstallation installation = installationRepository.findByInstallationId(installationId).orElse(null);
        if (installation == null || repositoriesAdded == null || !repositoriesAdded.isArray()) {
            return;
        }
        for (JsonNode r : repositoriesAdded) {
            upsertRepo(installation, r.path("id").asLong(), r.path("full_name").asText(),
                r.has("html_url") ? r.path("html_url").asText(null) : "https://github.com/" + r.path("full_name").asText());
        }
    }

    @Transactional
    public void removeReposFromWebhook(long installationId, JsonNode repositoriesRemoved) {
        GithubInstallation installation = installationRepository.findByInstallationId(installationId).orElse(null);
        if (installation == null || repositoriesRemoved == null || !repositoriesRemoved.isArray()) {
            return;
        }
        for (JsonNode r : repositoriesRemoved) {
            long repoId = r.path("id").asLong();
            repoRepository.findByInstallationIdAndGithubRepoId(installation.getId(), repoId)
                .ifPresent(repoRepository::delete);
        }
    }

    @Transactional(readOnly = true)
    public List<InstallationView> listInstallations(UUID userId, UUID workspaceId) {
        requireAdmin(userId, workspaceId);
        List<GithubInstallation> installs = installationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<InstallationView> result = new ArrayList<>();
        for (GithubInstallation inst : installs) {
            List<RepoView> repos = repoRepository.findByInstallationId(inst.getId()).stream()
                .map(r -> new RepoView(
                    r.getId(),
                    r.getGithubRepoId(),
                    r.getFullName(),
                    r.getHtmlUrl(),
                    r.getProject() != null ? r.getProject().getId() : null,
                    r.getProject() != null ? r.getProject().getName() : null
                ))
                .toList();
            result.add(new InstallationView(
                inst.getId(),
                inst.getInstallationId(),
                inst.getAccountLogin(),
                inst.getAccountType(),
                repos
            ));
        }
        return result;
    }

    @Transactional
    public RepoView mapRepoToProject(UUID userId, UUID workspaceId, UUID repoId, UUID projectId) {
        requireAdmin(userId, workspaceId);
        GithubRepo repo = repoRepository.findById(repoId)
            .orElseThrow(() -> new ResourceNotFoundException("GithubRepo", repoId));
        if (!repo.getInstallation().getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Repo does not belong to this workspace");
        }

        if (projectId == null) {
            repo.setProject(null);
        } else {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
            if (!project.getWorkspace().getId().equals(workspaceId)) {
                throw new BadRequestException("Project does not belong to this workspace");
            }
            repo.setProject(project);
        }
        repo = repoRepository.save(repo);
        return new RepoView(
            repo.getId(),
            repo.getGithubRepoId(),
            repo.getFullName(),
            repo.getHtmlUrl(),
            repo.getProject() != null ? repo.getProject().getId() : null,
            repo.getProject() != null ? repo.getProject().getName() : null
        );
    }

    @Transactional
    public void disconnectInstallation(UUID userId, UUID workspaceId, UUID installationUuid) {
        requireAdmin(userId, workspaceId);
        GithubInstallation inst = installationRepository.findById(installationUuid)
            .orElseThrow(() -> new ResourceNotFoundException("GithubInstallation", installationUuid));
        if (!inst.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Installation does not belong to this workspace");
        }
        installationRepository.delete(inst);
    }

    private void syncReposFromApi(GithubInstallation installation) {
        try {
            List<GithubApiClient.RepoInfo> repos = githubApiClient.listInstallationRepos(installation.getInstallationId());
            for (GithubApiClient.RepoInfo info : repos) {
                upsertRepo(installation, info.id(), info.fullName(), info.htmlUrl());
            }
        } catch (Exception e) {
            log.warn("Could not sync repos from GitHub API for installation {}: {}",
                installation.getInstallationId(), e.getMessage());
        }
    }

    private void syncReposFromPayload(GithubInstallation installation, JsonNode repositories) {
        for (JsonNode r : repositories) {
            upsertRepo(
                installation,
                r.path("id").asLong(),
                r.path("full_name").asText(),
                r.has("html_url") ? r.path("html_url").asText(null) : "https://github.com/" + r.path("full_name").asText()
            );
        }
    }

    private void upsertRepo(GithubInstallation installation, long githubRepoId, String fullName, String htmlUrl) {
        GithubRepo repo = repoRepository
            .findByInstallationIdAndGithubRepoId(installation.getId(), githubRepoId)
            .orElse(null);
        if (repo == null) {
            repo = GithubRepo.builder()
                .installation(installation)
                .githubRepoId(githubRepoId)
                .fullName(fullName)
                .htmlUrl(htmlUrl)
                .build();
        } else {
            repo.setFullName(fullName);
            if (htmlUrl != null) {
                repo.setHtmlUrl(htmlUrl);
            }
        }
        repoRepository.save(repo);
    }

    private void requireAdmin(UUID userId, UUID workspaceId) {
        Workspace ws = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        if (userId == null) {
            throw new ForbiddenException("Authentication required");
        }
        boolean isOwner = ws.getOwner().getId().equals(userId);
        boolean isAdmin = memberRepository.findById(new WorkspaceMemberId(ws.getId(), userId))
            .map(m -> "WORKSPACE_ADMIN".equals(m.getRole().getName()))
            .orElse(false);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only workspace owner or admins can manage GitHub integration");
        }
    }

    public record InstallationView(
        UUID id,
        Long installationId,
        String accountLogin,
        String accountType,
        List<RepoView> repos
    ) {}

    public record RepoView(
        UUID id,
        Long githubRepoId,
        String fullName,
        String htmlUrl,
        UUID projectId,
        String projectName
    ) {}
}
