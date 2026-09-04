package com.example.demo.chat.application;

import com.example.demo.chat.api.response.UnreadResponse;
import com.example.demo.chat.application.dto.ChatReadSnapshot;
import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatReadState;
import com.example.demo.chat.domain.ChatReadStateRepository;
import jakarta.validation.constraints.Min;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatReadService {

    private final ChatMessageService messageService;
    private final ChatReadStateRepository stateRepository;

    public ChatReadService(ChatMessageService messageService, ChatReadStateRepository stateRepository) {
        this.messageService = messageService;
        this.stateRepository = stateRepository;
    }

    public List<ChatMessage> readPage(String roomId, long cursor, int size) {
        return messageService.findAfter(roomId, cursor, size);
    }

    /**
     * 채팅방 사용자별 읽음 데이터를 MySQL 에 저장하고, 저장된 읽음 위치는 이전 값보다 작아지지 않는다.
     * 즉 요청한 messageId가 저장값보다 크면 갱신하고,
     * 작거나 같으면 기존 저장값을 유지한 뒤 최종 저장 상태를 반환한다.
     */
    @Transactional
    public ChatReadSnapshot markRead(String roomId, String memberId, long lastReadId) {
        // TODO 1: 방·사용자 조합의 ChatReadState를 조회하거나 새로 만드세요.
        ChatReadState state = stateRepository.findByRoomIdAndMemberId(roomId, memberId)
            .orElse (new ChatReadState(roomId, memberId));

        // TODO 2: Entity의 updateLastReadId로 Cursor 역행을 막으세요.

//        if (state.getLastReadId() < lastReadId) {
//            state.updateLastReadId();
//        }
        state.updateLastReadId(lastReadId);

        // TODO 3: 상태를 저장하고 ChatReadSnapshot으로 반환하세요.

        stateRepository.save(state);

        return snapshot(state);

//        throw new UnsupportedOperationException("특정 사용자가 한 번 읽은 데이터의 상태보다 더 과거의 데이터는 처리되지 않도록 해야 함.");
    }

    @NonNull
    private static ChatReadSnapshot snapshot(ChatReadState state) {
        return new ChatReadSnapshot(state.getRoomId(), state.getMemberId(), state.getLastReadId());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String roomId, String memberId) {
        long lastReadId = stateRepository.findByRoomIdAndMemberId(roomId, memberId)
            .map(ChatReadState::getLastReadId)
            .orElse(0L);

        return messageService.countAfter(roomId, memberId, lastReadId);
    }
}
