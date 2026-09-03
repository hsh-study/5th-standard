package com.example.demo.chat.application.dto;

public record ChatReadSnapshot(
    String roomId, String memberId, long lastReadId
) {
}
