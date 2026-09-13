package com.example.jari.issue.dto;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class CommentResponse {
    private UUID id;
    private UUID authorId;
    private String authorName;
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
