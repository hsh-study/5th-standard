package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatReadSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChatReadServiceTest {
    @Autowired
    private ChatMessageService messages;

    @Autowired
    private ChatReadService readService;

    @Test
    void 마지막으로_읽은_메시지_id는_뒤로_가지_않는다() {
        var first = messages.send("room-1", "member-2", "read-1", "first");
        var last = messages.send("room-1", "member-2", "read-2", "last");

        readService.markRead("room-1", "member-1", last.getId());
        ChatReadSnapshot result = readService.markRead("room-1", "member-1", first.getId());

        assertThat(result.lastReadId()).isEqualTo(last.getId());
    }

    @Test
    void 내가_보낸_메시지는_unread_count에서_제외한다() {
        messages.send("room-1", "member-1", "c1", "mine");
        messages.send("room-1", "member-2", "c2", "theirs");

        assertThat(readService.unreadCount("room-1", "member-1")).isEqualTo(1);
    }
}
