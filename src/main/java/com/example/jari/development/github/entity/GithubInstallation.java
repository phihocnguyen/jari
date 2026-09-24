package com.example.jari.development.github.entity;

import com.example.jari.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "github_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "installation_id", nullable = false, unique = true)
    private Long installationId;

    @Column(name = "account_login", nullable = false, length = 255)
    private String accountLogin;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @OneToMany(mappedBy = "installation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GithubRepo> repos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
