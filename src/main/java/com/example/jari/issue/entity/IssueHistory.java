package com.example.jari.issue.entity;
import com.example.jari.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "issue_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IssueHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id") private Issue issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")  private User user;
    @Column(nullable = false, length = 50) private String field;
    @Column(name = "old_value", columnDefinition = "TEXT") private String oldValue;
    @Column(name = "new_value", columnDefinition = "TEXT") private String newValue;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
}
