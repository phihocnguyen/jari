package com.example.jari.project.entity;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProjectMemberId implements Serializable {
    private UUID projectId;
    private UUID userId;
}
