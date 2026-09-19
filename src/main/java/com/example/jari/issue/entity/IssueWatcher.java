package com.example.jari.issue.entity;

import com.example.jari.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "issue_watchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueWatcher {

    @EmbeddedId
    private IssueWatcherId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("issueId")
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
