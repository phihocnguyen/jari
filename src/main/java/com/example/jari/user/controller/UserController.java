package com.example.jari.user.controller;

import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import com.example.jari.user.dto.UpdateProfileRequest;
import com.example.jari.user.dto.UserResponse;
import com.example.jari.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Endpoints for user profile management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile information of the currently authenticated user."
    )
    @GetMapping("/me")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(userService.getProfile(user.getId())));
    }

    @Operation(
        summary = "Update current user profile",
        description = "Updates the display name or avatar URL of the currently authenticated user."
    )
    @PutMapping("/me")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(userService.updateProfile(user.getId(), request)));
    }
}
