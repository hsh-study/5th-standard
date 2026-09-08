package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageResponse;
import com.example.demo.chat.application.dto.ChatReadSnapshot;
import com.example.demo.chat.application.dto.CursorResponse;
import com.example.demo.chat.application.dto.HistoryResponse;
import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import com.example.demo.chat.domain.ChatReadState;
import com.example.demo.chat.domain.ChatReadStateRepository;
import com.example.demo.chat.infra.ChatMessageRepositoryImpl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatReadService {

    private final ChatMessageRepository messageRepository;

    private final ChatMessageService messageService;
    private final ChatReadStateRepository stateRepository;

    public ChatReadService(
        ChatMessageRepository messageRepository,
        ChatMessageService messageService,
        ChatReadStateRepository stateRepository) {

        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.stateRepository = stateRepository;
    }

    public List<ChatMessage> readPage(String roomId, long cursor, int size) {
        return messageService.findAfter(roomId, cursor, size);
    }

    public CursorResponse<ChatMessageResponse> search(String roomId, long cursor, int size) {
        return messageService.search(roomId, cursor, size);
    }

    /**
     * 채팅방 사용자별 읽음 데이터를 MySQL 에 저장하고, 저장된 읽음 위치는 이전 값보다 작아지지 않는다.
     * 즉 요청한 messageId가 저장값보다 크면 갱신하고,
     * 작거나 같으면 기존 저장값을 유지한 뒤 최종 저장 상태를 반환한다.
     */
    @Transactional
    public ChatReadSnapshot markRead(String roomId, String memberId, long lastReadId) {
        // 방·사용자 조합의 ChatReadState를 조회하거나 새로 만드세요.
        ChatReadState state = stateRepository.findByRoomIdAndMemberId(roomId, memberId)
            .orElse (new ChatReadState(roomId, memberId));

        // Entity의 updateLastReadId로 Cursor 역행을 막으세요.
        state.updateLastReadId(lastReadId);

        // 상태를 저장하고 ChatReadSnapshot으로 반환하세요.
        stateRepository.save(state);

        return snapshot(state);
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

    /**
     * offset + Page 방식으로 메시지 목록 처리
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> offsetPage(String roomId, int page, int size) {
        return messageRepository.offsetPage(roomId, page, size);
    }

    /**
     * offset + Slice 방식으로 메시지 목록 처리
     */
    public Slice<ChatMessageResponse> offsetSlice(String roomId, int page, int size) {
        return messageRepository.offsetSlice(roomId, page, size);
    }

    /**
     * roomId, memberId에 해당하는 채팅방의 읽은 상태를 조회
     */
    @Transactional(readOnly = true)
    public ChatReadSnapshot readState(String roomId, String memberId) {
        return stateRepository.findByRoomIdAndMemberId(roomId, memberId)
            .map(state ->
                new ChatReadSnapshot(state.getRoomId(), state.getMemberId(), state.getLastReadId()))
            .orElse(new ChatReadSnapshot(roomId, memberId, 0L));
    }

    /**
     * roomId에 해당하는 채팅방의 size 만큼의 과거 메시지 목록 조회
     * TODO - beforeCursor 처리
     */
    @Transactional(readOnly = true)
    public HistoryResponse<ChatMessageResponse> history(String roomId, int size, long cursor) {
        List<ChatMessageResponse> messages = messageRepository.history(roomId, size, cursor);

        boolean hasPrevious = messages.size() > size;
        List<ChatMessageResponse> items = new ArrayList<>(messages.subList(0, Math.min(size, messages.size())));
        Collections.reverse(items);

        return new HistoryResponse<>(items, items.get(0).id(), hasPrevious);
    }
}
