package com.example.demo.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, Long> {
    Optional<ChatReadState> findByRoomIdAndMemberId(String roomId, String memberId);

}
