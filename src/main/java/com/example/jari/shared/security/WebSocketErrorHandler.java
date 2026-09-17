package com.example.jari.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String errorMessage = (cause.getMessage() != null && !cause.getMessage().isBlank())
                ? cause.getMessage()
                : ex.getMessage();

        log.warn("STOMP message processing error: {}", errorMessage);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorMessage);
        accessor.setLeaveMutable(true);

        byte[] payload = errorMessage != null ? errorMessage.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}
