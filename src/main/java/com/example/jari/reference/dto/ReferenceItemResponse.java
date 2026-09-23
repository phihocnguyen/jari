package com.example.jari.reference.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceItemResponse {
    private UUID id;
    private String name;
    private String description;
    private String extra;  // category for statuses, level for priorities
}
