package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repository;

    public ChatMessageService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    /**
     * 같은 roomId + clientMessageId 재전송을 기존 DB 메시지로 수렴시킨다.
     * 완료 조건: 재시도 결과나 INSERT 저장 모두 같은 messageId를 반환한다.
     */
    @Transactional
    public ChatMessage send(String roomId, String senderId, String clientMessageId, String content) {

        // TODO 1: 저장 전에 roomId + clientMessageId로 기존 메시지를 조회하세요.
        return repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
            .orElseGet(() ->
                // TODO 2: 처음 확인된 요청만 INSERT 합니다.
                repository.save(new ChatMessage(
                UUID.randomUUID(),
                roomId,
                senderId,
                clientMessageId,
                content,
                Instant.now()
            )));

    }

    @Transactional(readOnly = true)
    public List<ChatMessage> findAfter(String roomId, long afterSequence, int size) {
        return repository.findByRoomIdAndIdGreaterThanOrderByIdAsc(
                roomId,
                afterSequence,
                PageRequest.of(0, Math.max(1, Math.min(size, 100))))
            .stream()
            .toList();
    }

    @Transactional(readOnly = true)
    public long countAfter(String roomId, String memberId, long lastReadId) {
        return repository.countAfter(roomId, memberId, lastReadId);
    }
}
