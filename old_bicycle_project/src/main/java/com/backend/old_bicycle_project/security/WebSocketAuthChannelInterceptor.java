package com.backend.old_bicycle_project.security;

import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
            return message;
        }

        if (requiresAuthenticatedUser(accessor.getCommand()) && accessor.getUser() == null) {
            throw new AccessDeniedException("Unauthenticated WebSocket session");
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = resolveAuthorizationHeader(accessor);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing WebSocket bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new AccessDeniedException("Invalid WebSocket bearer token");
        }

        String email = jwtTokenProvider.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found for WebSocket session"));

        accessor.setUser(new StompUserPrincipal(user.getId().toString(), user.getEmail()));
        log.debug("Authenticated WebSocket session for user {}", user.getId());
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        String[] headerCandidates = {"Authorization", "authorization", "X-Authorization", "x-authorization"};
        for (String headerName : headerCandidates) {
            String headerValue = accessor.getFirstNativeHeader(headerName);
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue.trim();
            }
        }
        return null;
    }

    private boolean requiresAuthenticatedUser(StompCommand command) {
        return StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.UNSUBSCRIBE.equals(command);
    }
}
