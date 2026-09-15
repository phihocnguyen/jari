package com.example.jari.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReleaseRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 2000)
    private String description;
    private LocalDate releaseDate;
}
