package com.example.jari.issue.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank private String content;
}
