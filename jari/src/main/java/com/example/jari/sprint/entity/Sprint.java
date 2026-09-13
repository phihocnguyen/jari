package com.example.jari.sprint.entity;

import com.example.jari.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "sprints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sprint {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @Column(nullable = false, length = 100) private String name;
    @Column(columnDefinition = "TEXT") private String goal;
    @Column(name = "start_date") private OffsetDateTime startDate;
    @Column(name = "end_date")   private OffsetDateTime endDate;
    @Column(nullable = false, length = 20) @Enumerated(EnumType.STRING) private SprintStatus status = SprintStatus.PLANNED;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                    private OffsetDateTime updatedAt;
}
