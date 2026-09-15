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

    @Modifying
    @Query("DELETE FROM SprintIssue si WHERE si.id.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);
}
