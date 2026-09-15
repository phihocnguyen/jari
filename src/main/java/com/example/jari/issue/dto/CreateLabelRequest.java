package com.example.jari.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLabelRequest {
    @NotBlank @Size(max = 50)
    private String name;
    @Size(max = 20)
    private String color;
}
