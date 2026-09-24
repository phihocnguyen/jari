package com.example.jari.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * STOMP payload for live comment updates on {@code /topic/issues/{issueId}/comments}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent {
    /** CREATED | UPDATED | DELETED */
    private String type;
    private UUID issueId;
    private UUID commentId;
    private CommentResponse comment;
}
