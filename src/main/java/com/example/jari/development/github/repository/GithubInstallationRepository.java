package com.example.jari.development.github.repository;

import com.example.jari.development.github.entity.GithubInstallation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubInstallationRepository extends JpaRepository<GithubInstallation, UUID> {
    @EntityGraph(attributePaths = {"workspace"})
    Optional<GithubInstallation> findByInstallationId(Long installationId);

    List<GithubInstallation> findByWorkspaceId(UUID workspaceId);

    boolean existsByWorkspaceIdAndInstallationId(UUID workspaceId, Long installationId);
}
