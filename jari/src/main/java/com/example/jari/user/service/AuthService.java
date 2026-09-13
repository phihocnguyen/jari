package com.example.jari.user.service;

import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.shared.security.JwtTokenProvider;
import com.example.jari.user.dto.*;
import com.example.jari.user.entity.User;
import com.example.jari.user.entity.UserStatus;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already in use: " + request.getUsername());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName())
            .status(UserStatus.ACTIVE)
            .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new com.example.jari.shared.exception.ApiException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token");
        }
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        if (!tokenService.isRefreshTokenValid(userId, refreshToken)) {
            throw new com.example.jari.shared.exception.ApiException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Refresh token expired or revoked");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return buildAuthResponse(user);
    }

    public void logout(UUID userId, String accessToken) {
        tokenService.deleteRefreshToken(userId);
        long expiry = jwtTokenProvider.getAccessTokenExpirySeconds();
        tokenService.blacklistAccessToken(accessToken, expiry);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        tokenService.saveRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirySeconds());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
            .user(userMapper.toResponse(user))
            .build();
    }
}
