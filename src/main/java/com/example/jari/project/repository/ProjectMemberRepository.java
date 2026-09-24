package com.example.jari.project.repository;

import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByIdProjectId(UUID projectId);
    boolean existsByIdProjectIdAndIdUserId(UUID projectId, UUID userId);

    @Query("SELECT DISTINCT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId")
    List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.project.workspace.id = :workspaceId")
    List<ProjectMember> findAllByUserIdAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);

    @Query("""
        SELECT pm FROM ProjectMember pm
        JOIN FETCH pm.project
        WHERE pm.project.workspace.id = :workspaceId
        """)
    List<ProjectMember> findAllByWorkspaceIdWithProject(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.project.workspace.id = :workspaceId")
    void deleteAllByUserIdAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);
}
