package com.example.demo.chat.api;

import com.example.demo.chat.application.ChatRoomAccessService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/live-sales/{liveSaleId}/chat")
public class ChatRoomController {

    private final ChatRoomAccessService accessService;

    public ChatRoomController(ChatRoomAccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/join")
    public ChatRoomAccessService.Membership join(
            @PathVariable String liveSaleId,
            Principal principal
    ) {
        return accessService.join(liveSaleId, principal.getName());
    }
}
