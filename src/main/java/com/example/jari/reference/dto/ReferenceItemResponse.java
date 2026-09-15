package com.example.jari.reference.dto;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class ReferenceItemResponse {
    private UUID id;
    private String name;
    private String description;
    private String extra;  // category for statuses, level for priorities
}
