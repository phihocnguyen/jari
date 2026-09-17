package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.issueKey, LENGTH(i.project.projectKey) + 2) AS int)), 0) FROM Issue i WHERE i.project.id = :projectId")
    int findMaxIssueNumber(@Param("projectId") UUID projectId);

    Optional<Issue> findByProjectIdAndIssueKey(UUID projectId, String issueKey);

    Optional<Issue> findByIssueKeyIgnoreCase(String issueKey);

    @Modifying
    @Query("UPDATE Issue i SET i.parent = null WHERE i.parent.id = :issueId")
    void detachParentFromChildIssues(@Param("issueId") UUID issueId);

    @Modifying
    @Query("UPDATE Issue i SET i.release = null WHERE i.release.id = :releaseId")
    void detachReleaseFromIssues(@Param("releaseId") UUID releaseId);

    /**
     * Open issues whose due date is on/before {@code maxDueDate} (due soon or overdue),
     * excluding completed/cancelled work (status category 'DONE').
     * Project and assignee are fetch-joined because the reminder reads them after detaching.
     */
    @Query("SELECT i FROM Issue i JOIN FETCH i.project JOIN FETCH i.assignee " +
           "WHERE i.dueDate IS NOT NULL AND i.dueDate <= :maxDueDate " +
           "AND i.assignee IS NOT NULL AND (i.status IS NULL OR i.status.category <> 'DONE')")
    List<Issue> findIssuesDueSoon(@Param("maxDueDate") LocalDate maxDueDate);
}
