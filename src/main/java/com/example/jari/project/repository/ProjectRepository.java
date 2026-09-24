package com.example.jari.project.repository;
import com.example.jari.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspaceId(UUID workspaceId);
    boolean existsByWorkspaceIdAndProjectKey(UUID workspaceId, String projectKey);
    Optional<Project> findByProjectKeyIgnoreCase(String projectKey);

    /** Projects visible because the user owns the workspace or is WORKSPACE_ADMIN. */
    @Query("""
        SELECT p.id FROM Project p
        WHERE p.workspace.owner.id = :userId
           OR EXISTS (
             SELECT 1 FROM WorkspaceMember wm
             WHERE wm.workspace = p.workspace
               AND wm.user.id = :userId
               AND UPPER(wm.role.name) = 'WORKSPACE_ADMIN'
           )
        """)
    List<UUID> findProjectIdsVisibleAsWorkspaceAdmin(@Param("userId") UUID userId);
}

