package com.example.jari.automation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunAutomationRequest {
    @NotBlank(message = "Rule code is required")
    private String rule; // 'AUTO_CLOSE_PARENT', 'AUTO_ASSIGN_ME'
}
