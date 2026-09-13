package com.example.jari.user.controller;

import com.example.jari.shared.response.ApiResponse;
import com.example.jari.shared.security.CustomUserDetails;
import com.example.jari.user.dto.*;
import com.example.jari.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new account",
        description = "Creates a new user account with username, email and password. Returns JWT access + refresh tokens."
    )
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.example.jari.shared.response.ApiResponse.ok(authService.register(request)));
    }

    @Operation(
        summary = "Login with email and password",
        description = "Authenticates a user with email/password credentials. Returns JWT access + refresh tokens on success."
    )
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(authService.login(request)));
    }

    @Operation(
        summary = "Refresh access token",
        description = "Issues a new access token using a valid refresh token. The old refresh token remains valid until expiry."
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(
        summary = "Logout",
        description = "Invalidates the current access token by blacklisting it in Redis, and removes the refresh token."
    )
    @PostMapping("/logout")
    public ResponseEntity<com.example.jari.shared.response.ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(user.getId(), token);
        return ResponseEntity.ok(com.example.jari.shared.response.ApiResponse.ok("Logged out successfully"));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return "";
    }
}
