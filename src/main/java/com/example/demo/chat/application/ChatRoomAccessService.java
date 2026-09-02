package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.Membership;
import com.example.demo.chat.domain.ChatParticipant;
import com.example.demo.chat.domain.ChatParticipantRepository;
import com.example.demo.chat.domain.ChatRoom;
import com.example.demo.chat.domain.ChatRoomRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatRoomAccessService {

    private final ChatRoomRepository roomRepository;
    private final ChatParticipantRepository participantRepository;

    public ChatRoomAccessService(
        ChatRoomRepository roomRepository,
        ChatParticipantRepository participantRepository
    ) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public Membership join(String liveSaleId, String memberId) {
        roomRepository.findById(liveSaleId)
            .orElseGet(() -> roomRepository.save(new ChatRoom(liveSaleId, Instant.now())));
        if (!participantRepository.existsByRoomIdAndMemberId(liveSaleId, memberId)) {
            participantRepository.saveAndFlush(new ChatParticipant(liveSaleId, memberId, Instant.now()));
        }
        Set<String> participants = participantRepository.findAllByRoomIdOrderByIdAsc(liveSaleId).stream()
            .map(ChatParticipant::memberId)
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
