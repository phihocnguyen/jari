package com.example.jari.workspace.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class WorkspaceMemberId implements Serializable {
    private UUID workspaceId;
    private UUID userId;
}
