package com.example.jari.issue.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IssueWatcherId implements Serializable {
    private UUID issueId;
    private UUID userId;
}
