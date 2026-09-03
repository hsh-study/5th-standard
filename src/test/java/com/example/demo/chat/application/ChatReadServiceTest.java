package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatReadSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ChatMessageService.class, ChatReadService.class})
public class ChatReadServiceTest {
    @Autowired
    private ChatMessageService messages;

    @Autowired
    private ChatReadService reads;

    @Test
    void 마지막으로_읽은_메시지_id는_뒤로_가지_않는다() {
        reads.markRead("room-1", "member-1", 10);
        ChatReadSnapshot result = reads.markRead("room-1", "member-1", 3);

        assertThat(result.lastReadId()).isEqualTo(10);
    }

    @Test
    void 내가_보낸_메시지는_unread_count에서_제외한다() {
        messages.send("room-1", "member-1", "c1", "mine");
        messages.send("room-1", "member-2", "c2", "theirs");

        assertThat(reads.unreadCount("room-1", "member-1")).isEqualTo(1);
    }
}
