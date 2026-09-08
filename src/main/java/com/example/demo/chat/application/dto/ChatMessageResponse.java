package com.example.demo.chat.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    Long id,
    UUID messageId,
    String roomId,
    String senderId,
    String clientMessageId,
    String content,
    Instant sentAt
) {
}
