package com.example.jari.project.entity;

import com.example.jari.rbac.entity.Role;
import com.example.jari.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity @Table(name = "project_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMember {
    @EmbeddedId private ProjectMemberId id;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("projectId") @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreationTimestamp @Column(name = "joined_at", updatable = false)
    private OffsetDateTime joinedAt;
}
