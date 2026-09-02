package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        // TODO 4: 저장 전에 roomId + clientMessageId로 기존 메시지를 조회하세요.
        // TODO 4: 처음 확인된 요청만 saveOrFindDuplicate로 보내세요.
        throw new UnsupportedOperationException("중복된 메시지 입니다.");
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

    private ChatMessage saveOrFindDuplicate(
        String roomId, String senderId, String clientMessageId, String content) {

        try {
            // flush까지 수행해 DB Unique Key 충돌을 이 Transaction 안에서 확인한다.
            return repository.saveAndFlush(new ChatMessage(
                UUID.randomUUID(), roomId, senderId, clientMessageId, content, Instant.now()
            ));
        } catch (DataIntegrityViolationException e) {
            // 다른 요청이 먼저 저장했다면 승자의 Row를 다시 읽는다.
            return repository.findByRoomIdAndClientMessageId(roomId, clientMessageId)
                .orElseThrow(() -> new IllegalStateException("메시지가 존재하지 않습니다.", e));
        }
    }

}
