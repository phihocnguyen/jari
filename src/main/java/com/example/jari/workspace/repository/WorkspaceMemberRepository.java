package com.example.jari.workspace.repository;

import com.example.jari.workspace.entity.WorkspaceMember;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    List<WorkspaceMember> findByIdWorkspaceId(UUID workspaceId);
    boolean existsByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);
}
