package com.example.jari.project.repository;
import com.example.jari.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspaceId(UUID workspaceId);
    boolean existsByWorkspaceIdAndProjectKey(UUID workspaceId, String projectKey);
}
