package com.example.demo.chat.api;

import com.example.demo.chat.api.request.CharReadRequest;
import com.example.demo.chat.api.response.UnreadResponse;
import com.example.demo.chat.application.ChatReadService;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.application.dto.ChatReadSnapshot;
import com.example.demo.chat.domain.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @GetMapping("/messages")
    public List<ChatMessage> messages(
        @PathVariable String liveSaleId,
        @RequestParam(defaultValue = "0") @Min(0) long cursor,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return readService.readPage(liveSaleId, cursor, size);
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
}
