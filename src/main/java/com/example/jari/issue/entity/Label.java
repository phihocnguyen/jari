package com.example.jari.issue.entity;

import com.example.jari.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "labels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Label {

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String color;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
}
