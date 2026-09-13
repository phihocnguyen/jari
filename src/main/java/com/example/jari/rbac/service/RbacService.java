package com.example.jari.rbac.service;

import com.example.jari.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;

    /**
     * Resolve a role by name, throws if not found.
     * Use this when assigning roles to workspace/project members.
     */
    public com.example.jari.rbac.entity.Role getRoleByName(String name) {
        return roleRepository.findByName(name)
            .orElseThrow(() -> new com.example.jari.shared.exception.ResourceNotFoundException("Role", name));
    }
}
