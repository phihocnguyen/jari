package com.example.jari.development.entity;

import com.example.jari.issue.entity.Issue;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_developments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueDevelopment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(nullable = false, length = 20)
    private String type; // 'COMMIT', 'PULL_REQUEST', 'BRANCH'

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 50)
    private String status; // 'OPEN', 'MERGED', 'CLOSED', etc.

    @Column(length = 100)
    private String author;

    /** GitHub PR number / commit SHA / branch name — used for webhook upserts. */
    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "github_repo_id")
    private Long githubRepoId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
