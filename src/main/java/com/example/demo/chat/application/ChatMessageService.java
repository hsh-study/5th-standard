package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageResponse;
import com.example.demo.chat.application.dto.CursorResponse;
import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository repository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 같은 roomId + clientMessageId 재전송을 기존 DB 메시지로 수렴시킨다.
     * 완료 조건: 재시도 결과나 INSERT 저장 모두 같은 messageId를 반환한다.
     */
    public ChatMessage send(String roomId, String senderId, String clientMessageId, String content) {

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        ChatMessage chatMessage = ChatMessage.create(
            UUID.randomUUID(),
            roomId,
            senderId,
            clientMessageId,
            content,
            Instant.now()
        );

        // INSERT 실패의 rollback 이 끝난 뒤 새 트랜잭션에서 저장된 메시지를 조회
        try {

            return transaction.execute(status ->
                repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
                    .orElseGet(() -> repository.save(chatMessage)));

        } catch (DataIntegrityViolationException e) {

            return transaction.execute(status ->
                repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
                    .orElseThrow(() -> e));
        }

    }

    private ChatMessage saveOrFindDuplicate(String roomId, String senderId, String clientMessageId, String content) {
        ChatMessage chatMessage = ChatMessage.create(
            UUID.randomUUID(),
            roomId,
            senderId,
            clientMessageId,
            content,
            Instant.now()
        );

        try {
            // 처음 확인된 요청만 INSERT 합니다.
            return repository.save(chatMessage);

        } catch (DataIntegrityViolationException e) {

            return repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
                .orElseThrow(() -> e);
        }
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
