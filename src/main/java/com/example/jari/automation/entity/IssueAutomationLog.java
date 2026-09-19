package com.example.jari.automation.entity;

import com.example.jari.issue.entity.Issue;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_automation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueAutomationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "executed_at", updatable = false)
    private OffsetDateTime executedAt;
}
