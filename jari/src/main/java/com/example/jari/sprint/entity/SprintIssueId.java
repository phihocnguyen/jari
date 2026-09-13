package com.example.jari.sprint.entity;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class SprintIssueId implements Serializable {
    private UUID sprintId;
    private UUID issueId;
}
