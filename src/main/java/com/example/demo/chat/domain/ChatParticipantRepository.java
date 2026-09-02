package com.example.demo.chat.domain;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    boolean existsByRoomIdAndMemberId(String roomId, String memberId);
    List<ChatParticipant> findAllByRoomIdOrderByIdAsc(String roomId);
}
