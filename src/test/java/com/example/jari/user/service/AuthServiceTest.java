package com.example.jari.user.service;

import com.example.jari.shared.exception.ApiException;
import com.example.jari.shared.exception.ConflictException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.shared.security.JwtTokenProvider;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.dto.*;
import com.example.jari.user.entity.User;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_throwsWhenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dev@example.com");
        req.setPassword("secret123");
        when(userRepository.existsByEmail("dev@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Email already in use");
    }

    @Test
    void register_generatesUsernameWhenMissing() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dev@example.com");
        req.setPassword("secret123");
        User saved = TestFixtures.user(UUID.randomUUID(), "Dev");
        UserResponse userResponse = UserResponse.builder().id(saved.getId()).email(saved.getEmail()).build();

        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        stubTokens(saved);
        when(userMapper.toResponse(saved)).thenReturn(userResponse);

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(userRepository).save(argThat(u -> u.getUsername().startsWith("dev_")));
    }

    @Test
    void register_throwsWhenUsernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dev@example.com");
        req.setPassword("secret123");
        req.setUsername("devuser");

        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("devuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Username already in use");
    }

    @Test
    void login_authenticatesAndReturnsTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("dev@example.com");
        req.setPassword("secret123");
        User user = TestFixtures.user(UUID.randomUUID(), "Dev");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(org.springframework.security.core.Authentication.class));
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));
        stubTokens(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(user.getId()).build());

        AuthResponse response = authService.login(req);

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void refresh_rejectsInvalidToken() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("bad");
        when(jwtTokenProvider.isTokenValid("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(req))
            .isInstanceOf(ApiException.class)
            .extracting("status", "errorCode")
            .containsExactly(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }

    @Test
    void refresh_rejectsRevokedToken() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh");
        when(jwtTokenProvider.isTokenValid("refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("refresh")).thenReturn(userId);
        when(tokenService.isRefreshTokenValid(userId, "refresh")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(req))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("expired or revoked");
    }

    @Test
    void refresh_returnsNewTokensForValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = TestFixtures.user(userId, "Dev");
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh");

        when(jwtTokenProvider.isTokenValid("refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("refresh")).thenReturn(userId);
        when(tokenService.isRefreshTokenValid(userId, "refresh")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        stubTokens(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(userId).build());

        AuthResponse response = authService.refresh(req);

        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void logout_blacklistsAccessTokenAndDeletesRefresh() {
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(3600L);

        authService.logout(userId, "access-token");

        verify(tokenService).deleteRefreshToken(userId);
        verify(tokenService).blacklistAccessToken("access-token", 3600L);
    }

    @Test
    void register_usesEmailPrefixAsDisplayNameWhenMissing() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dev@example.com");
        req.setPassword("secret123");
        User saved = TestFixtures.user(UUID.randomUUID(), "dev");
        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getDisplayName()).isEqualTo("dev");
            return saved;
        });
        stubTokens(saved);
        when(userMapper.toResponse(saved)).thenReturn(UserResponse.builder().id(saved.getId()).build());

        authService.register(req);
    }

    @Test
    void refresh_throwsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh");
        when(jwtTokenProvider.isTokenValid("refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("refresh")).thenReturn(userId);
        when(tokenService.isRefreshTokenValid(userId, "refresh")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubTokens(User user) {
        when(jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(user.getId())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(3600L);
        when(jwtTokenProvider.getRefreshTokenExpirySeconds()).thenReturn(86400L);
    }
}
