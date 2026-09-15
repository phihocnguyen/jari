package com.example.jari.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String status;

    @JsonProperty("fullName")
    public String getFullName() {
        return displayName;
    }
}
