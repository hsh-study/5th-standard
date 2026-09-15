package com.example.demo.chat.api;

import com.example.demo.chat.api.request.ChatCommand;
import com.example.demo.chat.application.ChatDeliveryService;
import com.example.demo.chat.application.ChatMessageService;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.application.MessageRelay;
import com.example.demo.chat.domain.ChatMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;
    private final ChatRoomAccessService accessService;
    private final ChatDeliveryService deliveryService;

    @MessageMapping("/live-sales/{liveSaleId}/messages")
    public void send(
        @DestinationVariable String liveSaleId,
        @Valid ChatCommand command,
        Principal principal
    ) {
        accessService.checkParticipant(liveSaleId, principal.getName());
        ChatMessage chatMessage = messageService.send(
            liveSaleId, principal.getName(), command.clientMessageId(), command.content());

        deliveryService.publish(chatMessage);
    }

}
