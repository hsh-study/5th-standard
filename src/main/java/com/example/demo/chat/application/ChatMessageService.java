package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageResponse;
import com.example.demo.chat.application.dto.CursorResponse;
import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository repository;

    /**
     * 같은 roomId + clientMessageId 재전송을 기존 DB 메시지로 수렴시킨다.
     * 완료 조건: 재시도 결과나 INSERT 저장 모두 같은 messageId를 반환한다.
     */
    @Transactional
    public ChatMessage send(String roomId, String senderId, String clientMessageId, String content) {

        // 저장 전에 roomId + clientMessageId로 기존 메시지를 조회하세요.
        return repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
            .orElseGet(() ->
                // 처음 확인된 요청만 INSERT 합니다.
                repository.save(new ChatMessage(
                UUID.randomUUID(),
                roomId,
                senderId,
                clientMessageId,
                content,
                Instant.now()
            )));

    }

    /**
     * Querydsl을 적용한 Repository 메서드 사용으로 변경
     */
    @Transactional(readOnly = true)
    public CursorResponse<ChatMessageResponse> search(String roomId, long afterSequence, int size) {

        List<ChatMessageResponse> fetched = repository.search(roomId, afterSequence, size);

        boolean hasNext = fetched.size() > size;
        Long lastId = hasNext ? fetched.get(size - 1).id() : null;
        List<ChatMessageResponse> items = fetched.subList(0, Math.min(size, fetched.size()));


        return new CursorResponse<>(items, lastId, hasNext);
    }

    /**
     * Querydsl 적용전
     * @param roomId
     * @param afterSequence
     * @param size
     * @return
     */
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
