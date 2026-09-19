package com.example.jari.component.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComponentRequest {
    @Size(max = 100, message = "Component name must not exceed 100 characters")
    private String name;

    private String description;

    private UUID leadId;
}
