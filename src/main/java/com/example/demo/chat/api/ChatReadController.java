package com.example.demo.chat.api;

import com.example.demo.chat.api.request.CharReadRequest;
import com.example.demo.chat.application.dto.CursorResponse;
import com.example.demo.chat.application.dto.HistoryResponse;
import com.example.demo.chat.api.response.OffsetPageResponse;
import com.example.demo.chat.api.response.OffsetSliceResponse;
import com.example.demo.chat.api.response.UnreadResponse;
import com.example.demo.chat.application.ChatReadService;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.application.dto.ChatMessageResponse;
import com.example.demo.chat.application.dto.ChatReadSnapshot;
import com.example.demo.chat.domain.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/live-sales/{liveSaleId}/chat")
public class ChatReadController {

    private final ChatReadService readService;
    private final ChatRoomAccessService accessService;

    public ChatReadController(ChatReadService readService, ChatRoomAccessService accessService) {
        this.readService = readService;
        this.accessService = accessService;
    }

    @GetMapping("/messages-new")
    public List<ChatMessage> messages(
        @PathVariable String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) long cursor,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return readService.readPage(liveSaleId, cursor, size);
    }

    @GetMapping("/messages")
    public CursorResponse<ChatMessageResponse> messagesNew(
        @PathVariable String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) long cursor,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return readService.search(liveSaleId, cursor, size);
    }

    /**
     * Page 와 offset 을 이용한 메시지 목록
     */
    @GetMapping("/messages/offset-page")
    public OffsetPageResponse<ChatMessageResponse> offsetPage(
        @PathVariable(name = "liveSaleId") String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());

        Page<ChatMessageResponse> result = readService.offsetPage(liveSaleId, page, size);

        return new OffsetPageResponse<>(result.getContent(),
            page, size, result.getTotalElements(), result.getTotalPages(),
            result.hasNext(),
            result.hasNext() ? result.nextPageable().getPageNumber() : null);
    }

    /**
     * Slice 와 offset 을 이용한 메시지 목록
     */
    @GetMapping("/messages/offset-slice")
    public OffsetSliceResponse<ChatMessageResponse> offsetSlice(
        @PathVariable String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());

        Slice<ChatMessageResponse> result = readService.offsetSlice(liveSaleId, page, size);

        return new OffsetSliceResponse<>(result.getContent(),
            page, size, result.hasNext(),
            result.hasNext() ? result.nextPageable().getPageNumber() : null);
    }

    /**
     * 이전 메시지 목록을 조회
     */
    @GetMapping("/messages/history")
    public HistoryResponse<ChatMessageResponse> history(
        @PathVariable String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) long before,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());

        return readService.history(liveSaleId, size, before);
    }

    @PostMapping("/mark-read")
    public ChatReadSnapshot markRead(
        @PathVariable String liveSaleId,
        @Valid @RequestBody CharReadRequest request,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return readService.markRead(liveSaleId, principal.getName(), request.lastReadId());
    }

    @GetMapping("/unread-count")
    public UnreadResponse unreadCount(@PathVariable String liveSaleId, Principal principal) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return new UnreadResponse(readService.unreadCount(liveSaleId, principal.getName()));
    }

    /**
     * 채팅 읽음 상태를 조회
     */
    @GetMapping("/read-state")
    public ChatReadSnapshot readState(@PathVariable String liveSaleId, Principal principal) {
        accessService.checkParticipant(liveSaleId, principal.getName());

        return readService.readState(liveSaleId, principal.getName());
    }
}
