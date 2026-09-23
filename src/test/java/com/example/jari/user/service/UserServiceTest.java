package com.example.jari.user.service;

import com.example.jari.support.TestFixtures;
import com.example.jari.user.dto.UpdateProfileRequest;
import com.example.jari.user.dto.UserResponse;
import com.example.jari.user.entity.User;
import com.example.jari.user.mapper.UserMapper;
import com.example.jari.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private UserService userService;

    @Test
    void searchUsers_returnsEmptyForBlankQuery() {
        assertThat(userService.searchUsers("  ")).isEmpty();
        verify(userRepository, never()).searchUsers(any());
    }

    @Test
    void updateProfile_publishesEventWhenDisplayNameChanges() {
        UUID userId = UUID.randomUUID();
        User user = TestFixtures.user(userId, "Old");
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setDisplayName("New Name");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(userId).displayName("New Name").build());

        userService.updateProfile(userId, req);

        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void updateProfile_skipsEventForAvatarOnly() {
        UUID userId = UUID.randomUUID();
        User user = TestFixtures.user(userId, "Dev");
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setAvatarUrl("http://avatar");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(userId).build());

        userService.updateProfile(userId, req);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
