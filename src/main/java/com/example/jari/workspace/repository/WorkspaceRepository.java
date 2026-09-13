package com.example.jari.workspace.repository;

import com.example.jari.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsByWorkspaceKey(String workspaceKey);

    @Query("SELECT w FROM Workspace w JOIN WorkspaceMember wm ON wm.workspace = w WHERE wm.user.id = :userId")
    List<Workspace> findAllByMemberUserId(UUID userId);
}
