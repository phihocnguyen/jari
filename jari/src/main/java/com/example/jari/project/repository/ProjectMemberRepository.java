package com.example.jari.project.repository;
import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByIdProjectId(UUID projectId);
    boolean existsByIdProjectIdAndIdUserId(UUID projectId, UUID userId);
}
