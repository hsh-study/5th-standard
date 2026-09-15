package com.example.demo.chat.application.dto;

import com.example.demo.chat.domain.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageRecorded (
    UUID eventId,
    UUID messageId,
    String roomId,
    String senderId,
    String content,
    Instant sentAt,
    int schemaVersion) {

    public ChatMessageRecorded {
        if (eventId == null || messageId == null || !eventId.equals(messageId)
            || roomId == null || roomId.isBlank() || senderId == null || senderId.isBlank()
            || content == null || sentAt == null || schemaVersion != 1) {
            throw new IllegalArgumentException("잘못된 메시지 입니다.");
        }
    }

    public static ChatMessageRecorded from(ChatMessage message) {
        return new ChatMessageRecorded(message.getMessageId(), message.getMessageId(),
            message.getRoomId(), message.getSenderId(), message.getContent(), message.getSentAt(), 1);
    }
}
