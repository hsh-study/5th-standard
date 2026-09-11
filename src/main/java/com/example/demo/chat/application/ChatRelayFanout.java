package com.example.demo.chat.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRelayFanout {
    private final MessageRelay relay;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Relay에서 받은 메시지를 현재 Server의 WebSocket Session에 Fan-out한다.
     * DB 저장은 서비스가 담당하고 이 Component는 전달만 맡는다.
     */
    @PostConstruct
    void subscribe() {
        // Application 시작 시 Relay Consumer를 한 번 등록한다.
        relay.subscribe(message -> messagingTemplate.convertAndSend(

            // 어느 Server가 받았든 같은 채팅방 Topic으로 전달한다.
            "/topic/live-sales/" + message.getRoomId(),
            message
        ));
    }
}
