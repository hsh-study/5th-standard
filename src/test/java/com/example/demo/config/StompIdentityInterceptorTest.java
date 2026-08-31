package com.example.demo.config;

import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.config.StompIdentityInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompIdentityInterceptorTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final ChatRoomAccessService accessService = new ChatRoomAccessService();
    private final StompIdentityInterceptor interceptor =
            new StompIdentityInterceptor(jwtDecoder, accessService);
    private final MessageChannel channel = new ExecutorSubscribableChannel();

    @Test
    void connect_frame에서_사용자를_식별한다() {
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt("member-1"));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setNativeHeader("Authorization", "Bearer valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        Message<?> authenticated = interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(authenticated).getUser().getName()).isEqualTo("member-1");
    }

    @Test
    void 잘못된_token은_connect를_거절한다() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 참여하지_않은_라이브_채티방의_send를_거절한다() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/live-sales/sale-1/messages");
        headers.setUser(UsernamePasswordAuthenticationToken.authenticated("member-1", "n/a", List.of()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("먼저 참여");
    }

    @Test
    void 참여한_라이브_채티방의_send를_허용한다() {
        accessService.join("sale-1", "member-1");
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/live-sales/sale-1/messages");
        headers.setUser(UsernamePasswordAuthenticationToken.authenticated("member-1", "n/a", List.of()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        Message<?> authorized = interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(authorized).getDestination())
                .isEqualTo("/app/live-sales/sale-1/messages");
    }

    private Jwt jwt(String subject) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("valid-token")
                .header("alg", "HS256")
                .issuer("live-commerce-standard")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .claim("roles", List.of("MEMBER"))
                .build();
    }
}
