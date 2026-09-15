package com.example.jari.sprint.entity;

import com.example.jari.issue.entity.Issue;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "sprint_issues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SprintIssue {
    @EmbeddedId private SprintIssueId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("sprintId") @JoinColumn(name = "sprint_id") private Sprint sprint;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("issueId")  @JoinColumn(name = "issue_id")  private Issue issue;
    @Column(nullable = false, precision = 20, scale = 6) private BigDecimal position = BigDecimal.ZERO;
    @CreationTimestamp @Column(name = "added_at", updatable = false) private OffsetDateTime addedAt;
}
