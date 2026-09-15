package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import com.example.demo.chat.infra.ChatMessageRepositoryImpl;
import com.example.demo.core.config.QuerydslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatMessageServiceTest {

    @Autowired
    ChatMessageService service;

    @Test
    void 같은_채팅방의_같은_clientMessageId는_한_메시지로_수렴한다() {
        ChatMessage first = service.send("sale-1", "member-1", "client-1", "안녕하세요");
        ChatMessage duplicate = service.send("sale-1", "member-1", "client-1", "안녕하세요");

        assertThat(duplicate.getMessageId()).isEqualTo(first.getMessageId());
        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(service.findAfter("sale-1", 0, 30))
            .hasSize(1)
            .first()
            .extracting(ChatMessage::getMessageId)
            .isEqualTo(first.getMessageId());
    }

    @Test
    void 다른_채팅방은_같은_clientMessageId를_사용해도_새_메시지를_만든다() {
        ChatMessage saleOne = service.send("sale-1", "member-1", "client-1", "sale-1 message");
        ChatMessage saleTwo = service.send("sale-2", "member-1", "client-1", "sale-2 message");

        assertThat(saleTwo.getMessageId()).isNotEqualTo(saleOne.getMessageId());
        assertThat(saleOne.getId()).isPositive();
        assertThat(saleTwo.getId()).isGreaterThan(saleOne.getId());
    }
}
