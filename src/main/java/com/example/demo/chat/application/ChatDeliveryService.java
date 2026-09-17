package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.application.event.InspectionPublisher;
import com.example.demo.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatDeliveryService {
    private static final Logger log =
        LoggerFactory.getLogger(ChatDeliveryService.class);

    private final MessageRelay relay;
    private final InspectionPublisher publisher;

    public void publish(ChatMessage message) {
        try {

            relay.publish(message);

        } catch (RuntimeException ex) {
            log.error("메시지 릴레이 실패, messageId={}", message.getMessageId(), ex);
        }

        try {

            publisher.publish(ChatMessageRecorded.from(message));

        } catch (RuntimeException ex) {
            log.error("메시지 검사를 위한 이벤트 발생 실패, eventId={}", message.getMessageId(), ex);
        }
    }
}
