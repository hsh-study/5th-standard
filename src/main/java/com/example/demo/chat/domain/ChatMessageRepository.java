package com.example.demo.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByRoomIdAndClientMessageId(String roomId, String clientMessageId);

    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(
        String roomId,
        long afterSequence,
        Pageable pageable
    );
}
