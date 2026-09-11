package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;

import java.util.function.Consumer;

public interface MessageRelay {

    // 저장된 메시지를 전달 경로로 발행합니다.
    void publish(ChatMessage message);

    // 수신 메시지를 처리할 Java 콜백을 등록합니다.
    void subscribe(Consumer<ChatMessage> consumer);
}
