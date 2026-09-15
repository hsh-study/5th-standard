package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.Membership;
import com.example.demo.chat.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomAccessService {

    private final ChatRoomRepository roomRepository;
    private final ChatParticipantRepository participantRepository;

    @Transactional
    public Membership join(String liveSaleId, String memberId) {

        roomRepository.findById(liveSaleId)
            .orElseGet(() -> roomRepository.save(ChatRoom.create(liveSaleId, Instant.now())));
        if (!participantRepository.existsByRoomIdAndMemberId(liveSaleId, memberId)) {
            participantRepository.saveAndFlush(ChatParticipant.create(liveSaleId, memberId, Instant.now()));
        }
        Set<String> participants = participantRepository.findAllByRoomIdOrderByIdAsc(liveSaleId).stream()
            .map(ChatParticipant::getMemberId)
            .collect(Collectors.toUnmodifiableSet());
        return new Membership(liveSaleId, memberId, participants);
    }

    @Transactional(readOnly = true)
    public void checkParticipant(String liveSaleId, String memberId) {
        if (!roomRepository.existsById(liveSaleId)
            || !participantRepository.existsByRoomIdAndMemberId(liveSaleId, memberId)) {
            throw new AccessDeniedException("라이브 판매 채팅방에 먼저 참여해야 합니다.");
        }
    }

}
