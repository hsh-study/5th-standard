package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatRelayFanoutTest {

    @Test
    void 등록할때는_전송하지_않고_수신할때_해당방으로_전달한다() {
        // given
        // mock()으로 실제 전송을 실행하지 않고 호출 여부와 전달된 인자를 확인할 객체를 만든다.
        MessageRelay relay = mock(MessageRelay.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        ChatRelayFanout fanout = new ChatRelayFanout(relay, messaging);

        // when
        // 서버의 Fanout이 Relay에 메시지 수신 함수를 등록한다.
        fanout.subscribe();

        // then
        // 서버에 수신 함수가 한 번 등록됐고, 메시지를 받기 전에는 STOMP 전송이 없음을 확인한다.
        // ArgumentCaptor는 메서드에 전달된 인자를 보관한다. 여기서는 등록된 수신 함수를 가져온다.
        ArgumentCaptor<Consumer<ChatMessage>> callback = ArgumentCaptor.captor();
        // verify()는 subscribe()가 한번 호출됐음을 확인하고, capture()는 그때 전달된 함수를 보관한다.
        verify(relay).subscribe(callback.capture());

        // getAllValues()는 보관한 인자 전체를, getValue()는 마지막으로 보관한 인자를 반환한다.
        assertThat(callback.getAllValues()).hasSize(1);
        assertThat(callback.getValue()).isNotNull();

        // verifyNoInteractions()는 messaging의 메서드가 한번도 호출되지 않았음을 확인한다.
        verifyNoInteractions(messaging);

        // given
        // 서로 다른 채팅방에 전달할 메시지 두 건을 준비한다.
        ChatMessage first = ChatMessage.create(
                UUID.randomUUID(), "room-1", 42L,
                "member-1", "message-1", "hello", Instant.now()
        );
        ChatMessage second = ChatMessage.create(
                UUID.randomUUID(), "room-2", 43L,
                "member-2", "message-2", "world", Instant.now()
        );

        // when
        // getValue()로 등록된 함수를 가져오고 accept()로 실행해 서버가 메시지를 받은 상황을 재현한다.
        callback.getValue().accept(first);
        callback.getValue().accept(second);

        // then
        // ArgumentCaptor 두 개에 전송 목적지와 메시지를 각각 보관해 각 방에 맞게 전달됐음을 확인한다.
        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChatMessage> messages = ArgumentCaptor.forClass(ChatMessage.class);
        // times(2)는 정확히 두 번 호출됐음을 확인한다. capture()는 각 호출에 전달된 인자를 보관한다.
        verify(messaging, times(2)).convertAndSend(destinations.capture(), messages.capture());

        assertThat(destinations.getAllValues()).containsExactly(
                "/topic/live-sales/room-1",
                "/topic/live-sales/room-2"
        );
        assertThat(messages.getAllValues()).containsExactly(first, second);

        // verifyNoMoreInteractions()는 앞에서 확인한 두 번 외에 다른 메서드 호출이 없음을 확인한다.
        verifyNoMoreInteractions(messaging);
    }
}
