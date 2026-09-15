package com.example.jari.issue.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "priorities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Priority {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 50) private String name;
    @Column(nullable = false, unique = true) private Integer level;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
}
