package com.example.demo.chat.config;

import com.example.demo.auth.application.JwtAuthenticationFactory;
import com.example.demo.auth.application.JwtUtil;
import com.example.demo.chat.application.ChatRoomAccessService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompIdentityInterceptor implements ChannelInterceptor {


    private final JwtUtil jwtUtil;
    private final JwtAuthenticationFactory authenticationFactory;
    private final ChatRoomAccessService accessService;

    public StompIdentityInterceptor(
        JwtUtil jwtUtil,
        JwtAuthenticationFactory authenticationFactory,
        ChatRoomAccessService accessService
    ) {
        this.jwtUtil = jwtUtil;
        this.authenticationFactory = authenticationFactory;
        this.accessService = accessService;
    }

    /**
     * CONNECT에서 만든 Principal을 SEND·SUBSCRIBE까지 이어 붙이고
     * 인증된 사용자에게도 채티방 참가자 Destination 규칙을 적용한다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // TODO - 1: 수정 가능한 StompHeaderAccessor로 원본 Message를 감싸세요.
        // TODO - 2: CONNECT의 Authorization Header를 JWT Principal로 바꾸세요.
        // TODO - 3: Principal 없는 SEND·SUBSCRIBE를 거부하세요.
        // TODO - 4: Principal의 memberId로 Destination 접근 권한을 검사하세요.
        // TODO - 5: 변경한 Header를 포함한 새 Message를 반환하세요.
        return message;
    }

    private Authentication authenticateJwt(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("STOMP CONNECT에 Bearer JWT가 필요합니다.");
        }
        Claims claims = jwtUtil.validateToken(authorization.substring("Bearer ".length()).trim());
        if (claims == null) {
            throw new AccessDeniedException("유효하지 않은 STOMP JWT입니다.");
        }
        return authenticationFactory.create(claims);
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
