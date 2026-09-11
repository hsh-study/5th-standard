package com.example.demo.chat.api;

import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.application.dto.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/live-sales/{liveSaleId}/chat")
public class ChatRoomController {

    private final ChatRoomAccessService accessService;

    @PostMapping("/join")
    public Membership join(
            @PathVariable String liveSaleId,
            Principal principal
    ) {
        return accessService.join(liveSaleId, principal.getName());
    }
}
