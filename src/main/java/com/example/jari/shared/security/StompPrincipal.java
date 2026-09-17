package com.example.jari.shared.security;

import java.security.Principal;

/**
 * STOMP session principal. {@code name} is the user's UUID (same value as the
 * JWT subject), which lets the subscribe guard match destinations like
 * /topic/notifications/{userId} against the authenticated identity.
 */
public record StompPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
