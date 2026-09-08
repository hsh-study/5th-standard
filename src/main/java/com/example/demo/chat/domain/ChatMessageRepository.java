package com.example.demo.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageCustomRepository {

    Optional<ChatMessage> findByRoomIdAndClientMessageId(String roomId, String clientMessageId);

    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(
        String roomId,
        long cursor,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.roomId = :roomId
            AND m.id > :lastReadId
            AND m.senderId <> :memberId
        """)
    long countAfter(
        @Param("roomId") String roomId,
        @Param("memberId") String memberId,
        @Param("lastReadId") long lastReadId
    );
}
