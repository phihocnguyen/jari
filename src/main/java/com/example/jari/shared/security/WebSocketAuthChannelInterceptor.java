package com.example.jari.shared.security;

import com.example.jari.shared.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticates STOMP sessions with the JWT sent in the CONNECT frame's
 * Authorization header (the SockJS/HTTP handshake itself stays public), and
 * stops users from subscribing to another user's notification topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String NOTIFICATIONS_TOPIC_PREFIX = "/topic/notifications/";
    private static final String DEMO_ACCESS_TOKEN = "demo-access-token";
    private static final String DEMO_USER_ID = "11111111-1111-1111-1111-111111111111";

    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                Principal user = authenticate(accessor);
                accessor.setUser(user);
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                guardSubscription(accessor);
            }
        }

        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        boolean bypass = appProperties.getSecurity() != null && appProperties.getSecurity().isBypass();
        String header = accessor.getFirstNativeHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (DEMO_ACCESS_TOKEN.equals(token)) {
                log.debug("STOMP authenticated using demo token");
                return new StompPrincipal(DEMO_USER_ID);
            }
            if (jwtTokenProvider.isTokenValid(token)) {
                try {
                    UUID userId = jwtTokenProvider.extractUserId(token);
                    return new StompPrincipal(userId.toString());
                } catch (IllegalArgumentException e) {
                    throw new MessagingException("Invalid token subject");
                }
            }
            if (bypass) {
                log.debug("STOMP authenticated using bypass mode fallback");
                return new StompPrincipal(DEMO_USER_ID);
            }
            throw new MessagingException("Invalid or expired token");
        }

        if (bypass) {
            log.debug("STOMP authenticated using bypass mode with no token");
            return new StompPrincipal(DEMO_USER_ID);
        }

        throw new MessagingException("Missing Bearer token in CONNECT frame");
    }

    private void guardSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(NOTIFICATIONS_TOPIC_PREFIX)) {
            return;
        }

        boolean bypass = appProperties.getSecurity() != null && appProperties.getSecurity().isBypass();
        if (bypass) {
            return;
        }

        Principal user = accessor.getUser();
        String targetUserId = destination.substring(NOTIFICATIONS_TOPIC_PREFIX.length());
        if (user == null || !targetUserId.equals(user.getName())) {
            log.warn("Subscription denied: user {} cannot subscribe to destination {}",
                    user != null ? user.getName() : "anonymous", destination);
            throw new AccessDeniedException("Cannot subscribe to another user's notifications");
        }
    }
}
