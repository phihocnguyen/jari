package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID issueId);
}
