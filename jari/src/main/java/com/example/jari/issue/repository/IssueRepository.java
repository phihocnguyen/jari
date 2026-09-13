package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.issueKey, LENGTH(i.project.projectKey) + 2) AS int)), 0) FROM Issue i WHERE i.project.id = :projectId")
    int findMaxIssueNumber(UUID projectId);

    Optional<Issue> findByProjectIdAndIssueKey(UUID projectId, String issueKey);
}
