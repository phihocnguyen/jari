package com.example.jari.issue.entity;

import com.example.jari.project.entity.Project;
import com.example.jari.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "issues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Issue {

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "issue_key", nullable = false, length = 30)
    private String issueKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_type_id") private IssueType issueType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id") private Status status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "priority_id") private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id") private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id") private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") private Issue parent;

    @Column(name = "story_points", precision = 5, scale = 2)
    private BigDecimal storyPoints;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                    private OffsetDateTime updatedAt;
}
