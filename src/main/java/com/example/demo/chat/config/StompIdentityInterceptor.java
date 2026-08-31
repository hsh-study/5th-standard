package com.example.demo.chat.config;

import com.example.demo.chat.application.ChatRoomAccessService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompIdentityInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final ChatRoomAccessService accessService;

    public StompIdentityInterceptor(
            JwtDecoder jwtDecoder,
            ChatRoomAccessService accessService
    ) {
        this.jwtDecoder = jwtDecoder;
        this.accessService = accessService;
    }

    /**
     * CONNECT에서 만든 Principal을 SEND·SUBSCRIBE까지 이어 붙이고
     * 인증된 사용자에게도 채티방 참가자 Destination 규칙을 적용한다.
     * 완료 조건: 정상 JWT는 연결되고, Token 누락·허용되지 않은 Destination은 거부된다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // TODO - Step 1: 수정 가능한 StompHeaderAccessor로 원본 Message를 감싸세요.
        // TODO - Step 2: CONNECT의 Authorization Header를 JWT Principal로 바꾸세요.
        // TODO - Step 3: Principal 없는 SEND·SUBSCRIBE를 거부하세요.
        // TODO - Step 4: Principal의 memberId로 Destination 접근 권한을 검사하세요.
        // TODO - Step 5: 변경한 Header를 포함한 새 Message를 반환하세요.
        return message;
    }

    private AbstractAuthenticationToken authenticateJwt(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("STOMP CONNECT에 Bearer JWT가 필요합니다.");
        }
        Jwt jwt = jwtDecoder.decode(authorization.substring("Bearer ".length()));
        List<SimpleGrantedAuthority> authorities = jwt.getClaimAsStringList("roles").stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private void authorizeDestination(String destination, String memberId) {
        if (destination == null) {
            throw new AccessDeniedException("Destination이 필요합니다.");
        }
        if (destination.startsWith("/app/live-sales/") || destination.startsWith("/topic/live-sales/")) {
            accessService.checkParticipant(segmentAfter(destination, "live-sales"), memberId);
            return;
        }
        throw new AccessDeniedException("허용되지 않은 Destination입니다: " + destination);
    }

    private String segmentAfter(String destination, String marker) {
        String[] parts = destination.split("/");
        for (int index = 0; index < parts.length - 1; index++) {
            if (parts[index].equals(marker) && !parts[index + 1].isBlank()) {
                return parts[index + 1];
            }
        }
        throw new AccessDeniedException("허용되지 않은 Destination입니다: " + destination);
    }
}
