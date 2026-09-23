package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID issueId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        WHERE c.issue.id = :issueId AND c.deletedAt IS NULL
        ORDER BY c.createdAt ASC
        """)
    List<Comment> findByIssueIdWithAuthor(@Param("issueId") UUID issueId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.issue.id = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);
}
