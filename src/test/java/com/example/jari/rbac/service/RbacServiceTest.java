package com.example.jari.rbac.service;

import com.example.jari.rbac.entity.Role;
import com.example.jari.rbac.repository.RoleRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock private RoleRepository roleRepository;
    @InjectMocks private RbacService rbacService;

    @Test
    void getRoleByName_returnsRole() {
        Role role = Role.builder().name("WORKSPACE_ADMIN").build();
        when(roleRepository.findByName("WORKSPACE_ADMIN")).thenReturn(Optional.of(role));

        assertThat(rbacService.getRoleByName("WORKSPACE_ADMIN")).isEqualTo(role);
    }

    @Test
    void getRoleByName_throwsWhenMissing() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rbacService.getRoleByName("UNKNOWN"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
