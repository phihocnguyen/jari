package com.example.jari.sprint.repository;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintIssueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SprintIssueRepository extends JpaRepository<SprintIssue, SprintIssueId> {
    List<SprintIssue> findByIdSprintIdOrderByPositionAsc(UUID sprintId);

    @Query("""
        SELECT si FROM SprintIssue si
        JOIN FETCH si.issue i
        JOIN FETCH i.status
        JOIN FETCH i.issueType
        JOIN FETCH i.priority
        LEFT JOIN FETCH i.assignee
        LEFT JOIN FETCH i.reporter
        LEFT JOIN FETCH i.project
        LEFT JOIN FETCH i.parent
        LEFT JOIN FETCH i.release
        WHERE si.id.sprintId = :sprintId
        ORDER BY si.position ASC
        """)
    List<SprintIssue> findBySprintIdWithIssues(@Param("sprintId") UUID sprintId);

    @Modifying
    @Query("DELETE FROM SprintIssue si WHERE si.id.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);

    @Modifying
    @Query("DELETE FROM SprintIssue si WHERE si.id.sprintId = :sprintId")
    void deleteBySprintId(@Param("sprintId") UUID sprintId);
}
