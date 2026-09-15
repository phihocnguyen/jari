package com.example.jari.issue.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "issue_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IssueType {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 50) private String name;
    private String description;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
}
