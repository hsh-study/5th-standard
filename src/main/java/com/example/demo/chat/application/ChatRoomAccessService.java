package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatRoom;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRoomAccessService {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    public Membership join(String liveSaleId, String memberId) {
        ChatRoom room = rooms.computeIfAbsent(liveSaleId, ChatRoom::new);
        room.join(memberId);
        return new Membership(liveSaleId, memberId, room.participants());
    }

    public void checkParticipant(String liveSaleId, String memberId) {
        ChatRoom room = rooms.get(liveSaleId);
        if (room == null || !room.canAccess(memberId)) {
            throw new AccessDeniedException("판매 채팅방에 먼저 참여해야 합니다.");
        }
    }

    public record Membership(String liveSaleId, String memberId, Set<String> participants) {
    }
}
