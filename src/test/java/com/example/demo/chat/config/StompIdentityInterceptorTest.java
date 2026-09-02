package com.example.demo.chat.config;

import com.example.demo.auth.application.JwtAuthenticationFactory;
import com.example.demo.auth.application.JwtUtil;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.domain.ChatParticipantRepository;
import com.example.demo.chat.domain.ChatRoomRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompIdentityInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final JwtAuthenticationFactory authenticationFactory = new JwtAuthenticationFactory();
    private final ChatRoomRepository roomRepository = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository participantRepository = mock(ChatParticipantRepository.class);
    private final ChatRoomAccessService accessService = new ChatRoomAccessService(roomRepository, participantRepository);
    private final StompIdentityInterceptor interceptor =
        new StompIdentityInterceptor(jwtUtil, authenticationFactory, accessService);
    private final MessageChannel channel = new ExecutorSubscribableChannel();

    @Test
    void connect_frame에서_사용자를_식별한다() {
        Claims validClaims = claims("member-1");

        when(jwtUtil.validateToken("valid-token")).thenReturn(validClaims);

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
    void 참여하지_않은_채팅방의_send를_거절한다() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/live-sales/sale-1/messages");
        headers.setUser(UsernamePasswordAuthenticationToken.authenticated("member-1", "n/a", List.of()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("먼저 참여");
    }

    @Test
    void 참여한_채팅방의_send를_허용한다() {
        // ERROR-CODE accessService.join("sale-1", "member-1"); // 기존 오류 코드
        when(roomRepository.existsById("sale-1")).thenReturn(true);
        when(participantRepository.existsByRoomIdAndMemberId("sale-1", "member-1")).thenReturn(true);

        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/live-sales/sale-1/messages");
        headers.setUser(UsernamePasswordAuthenticationToken.authenticated("member-1", "n/a", List.of()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        Message<?> authorized = interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(authorized).getDestination())
                .isEqualTo("/app/live-sales/sale-1/messages");
    }

    private Claims claims(String subject) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("roles", List.class)).thenReturn(List.of("MEMBER"));
        return claims;
    }
}
