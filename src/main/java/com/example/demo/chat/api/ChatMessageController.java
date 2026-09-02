package com.example.demo.chat.api;

import com.example.demo.chat.api.request.ChatCommand;
import com.example.demo.chat.application.ChatMessageService;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.domain.ChatMessage;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatMessageController {

    private final ChatMessageService messageService;
    private final ChatRoomAccessService accessService;

    public ChatMessageController(ChatMessageService messageService, ChatRoomAccessService accessService) {
        this.messageService = messageService;
        this.accessService = accessService;
    }

    @MessageMapping("/live-sales/{liveSaleId}/messages")
    @SendTo("/topic/live-sales/{liveSaleId}")
    public ChatMessage send(
        @DestinationVariable String liveSaleId,
        @Valid ChatCommand command,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        return messageService.send(
            liveSaleId, principal.getName(), command.clientMessageId(), command.content());
    }

}
