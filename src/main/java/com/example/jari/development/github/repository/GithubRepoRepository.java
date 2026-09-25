package com.example.jari.development.github.repository;

import com.example.jari.development.github.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubRepoRepository extends JpaRepository<GithubRepo, UUID> {
    Optional<GithubRepo> findByGithubRepoId(Long githubRepoId);

    Optional<GithubRepo> findByInstallationIdAndGithubRepoId(UUID installationId, Long githubRepoId);

    List<GithubRepo> findByInstallation_WorkspaceId(UUID workspaceId);

    @Query("SELECT r FROM GithubRepo r JOIN FETCH r.installation LEFT JOIN FETCH r.project WHERE r.githubRepoId = :githubRepoId")
    Optional<GithubRepo> findByGithubRepoIdWithInstallation(@Param("githubRepoId") Long githubRepoId);

    @Query("SELECT r FROM GithubRepo r LEFT JOIN FETCH r.project WHERE r.installation.id = :installationId ORDER BY r.createdAt DESC")
    List<GithubRepo> findByInstallationId(@Param("installationId") UUID installationId);
}
