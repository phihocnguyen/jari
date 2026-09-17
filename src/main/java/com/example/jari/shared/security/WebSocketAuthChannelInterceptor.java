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
import org.springframework.messaging.support.MessageBuilder;
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

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Principal user = authenticate(accessor);
            accessor.setUser(user);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            guardSubscription(accessor);
        }

        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing Bearer token in CONNECT frame");
        }
        String token = header.substring(7);
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new MessagingException("Invalid or expired token");
        }
        try {
            UUID userId = jwtTokenProvider.extractUserId(token);
            return new StompPrincipal(userId.toString());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Invalid token subject");
        }
    }

    private void guardSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(NOTIFICATIONS_TOPIC_PREFIX)) {
            return;
        }
        Principal user = accessor.getUser();
        String targetUserId = destination.substring(NOTIFICATIONS_TOPIC_PREFIX.length());
        if (user == null || !targetUserId.equals(user.getName())) {
            throw new AccessDeniedException("Cannot subscribe to another user's notifications");
        }
    }
}
