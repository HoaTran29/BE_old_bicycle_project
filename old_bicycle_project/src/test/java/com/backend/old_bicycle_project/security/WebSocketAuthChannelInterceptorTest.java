package com.backend.old_bicycle_project.security;

import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    void connectWithValidBearerTokenSetsUserPrincipalToUserId() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("buyer@test.dev")
                .build();

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        Message<?> message = buildConnectMessage("Bearer valid-token");

        Message<?> intercepted = interceptor.preSend(message, mock(MessageChannel.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(intercepted);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        Message<?> message = buildConnectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing WebSocket bearer token");
    }

    @Test
    void connectWithLowercaseAuthorizationHeaderIsAccepted() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("seller@test.dev")
                .build();

        when(jwtTokenProvider.validateToken("lower-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("lower-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("authorization", "Bearer lower-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> intercepted = interceptor.preSend(message, mock(MessageChannel.class));
        StompHeaderAccessor interceptedAccessor = StompHeaderAccessor.wrap(intercepted);

        assertThat(interceptedAccessor.getUser()).isNotNull();
        assertThat(interceptedAccessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        Message<?> message = buildConnectMessage("Bearer bad-token");

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid WebSocket bearer token");
    }

    @Test
    void connectWithUnknownUserIsRejected() {
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token")).thenReturn("ghost@test.dev");
        when(userRepository.findByEmail("ghost@test.dev")).thenReturn(Optional.empty());

        Message<?> message = buildConnectMessage("Bearer valid-token");

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void sendFrameWithoutAuthenticatedUserIsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthenticated WebSocket session");
    }

    @Test
    void subscribeFrameWithoutAuthenticatedUserIsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthenticated WebSocket session");
    }

    private Message<?> buildConnectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
