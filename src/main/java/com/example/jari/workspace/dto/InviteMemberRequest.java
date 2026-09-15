package com.example.jari.workspace.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class InviteMemberRequest {
    @NotNull private UUID userId;
    @NotBlank private String roleName;
}
