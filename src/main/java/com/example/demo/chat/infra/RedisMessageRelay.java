package com.example.demo.chat.infra;

import com.example.demo.chat.application.MessageRelay;
import com.example.demo.chat.domain.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RedisMessageRelay implements MessageRelay {

    private static final String CHANNEL = "chat.messages";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer listenerContainer;

    // localSubscribers 는 Redis 메시지가 서버에 도착했을 때 실행할 함수(consumer)들을 보관합니다.
    private final List<Consumer<ChatMessage>> localSubscribers = new CopyOnWriteArrayList<>();

    // RedisMessageListenerContainer에 chat.messages Listener를 등록
    @PostConstruct
    void registerListener() {
        listenerContainer.addMessageListener(
            (message, pattern) -> onRedisMessage(
                new String(message.getBody(), StandardCharsets.UTF_8)
            ),
            new ChannelTopic(CHANNEL)
        );
    }

    // JSON 을 역직렬화 후 등록된 로컬 콜백에 전달
    public void onRedisMessage(String json) {
        try {
            ChatMessage message =
                objectMapper.readValue(json, ChatMessage.class);

            localSubscribers.forEach(
                subscriber -> subscriber.accept(message)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                "채팅 메시지 역직렬화 실패", exception
            );
        }
    }

    // 메시지를 JSON으로 발행
    @Override
    public void publish(ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                "채팅 메시지 직렬화 실패", exception
            );
        }
    }

    @Override
    public void subscribe(Consumer<ChatMessage> consumer) {
        localSubscribers.add(consumer);
    }

}
