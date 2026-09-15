package com.example.demo.chat.api;

import com.example.demo.chat.api.request.ChatCommand;
import com.example.demo.chat.application.ChatMessageService;
import com.example.demo.chat.application.ChatRoomAccessService;
import com.example.demo.chat.application.MessageRelay;
import com.example.demo.chat.domain.ChatMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatMessageControllerTest {

    // mock()은 실제 저장이나 발행을 실행하지 않고, 반환값을 정하거나 호출 여부를 확인할 수 있는 객체를 만든다.
    private final ChatMessageService service = mock(ChatMessageService.class);
    private final ChatRoomAccessService access = mock(ChatRoomAccessService.class);
    private final MessageRelay relay = mock(MessageRelay.class);

    private final ChatMessageController controller =
            new ChatMessageController(service, access, relay);

    private final ChatCommand command = new ChatCommand("message-1", "hello");

    @Test
    void 참여_확인과_저장_후_반환된_메시지를_한_번_발행한다() {
        // given
        // 저장 결과로 반환할 메시지를 준비한다.
        ChatMessage message = ChatMessage.create(
                UUID.randomUUID(), "room-1", 42L,
                "member-1", "message-1", "hello", Instant.now()
        );

        // when(...).thenReturn(...)은 지정한 인자로 send()가 호출되면 준비한 메시지를 반환하도록 설정한다.
        when(service.send("room-1", "member-1", "message-1", "hello"))
                .thenReturn(message);

        // when
        // member-1이 메시지를 전송한다.
        controller.send("room-1", command, () -> "member-1");

        // then
        // ArgumentCaptor는 메서드 호출에 전달된 인자를 보관한다. 여기서는 발행한 메시지를 가져온다.
        ArgumentCaptor<ChatMessage> published = ArgumentCaptor.forClass(ChatMessage.class);

        // inOrder()는 여러 객체의 메서드가 호출된 순서를 확인할 때 사용한다.
        var order = inOrder(access, service, relay);

        // order.verify()는 실제 메서드를 다시 실행하지 않고, 아래 순서대로 호출됐음을 확인한다.
        order.verify(access).checkParticipant("room-1", "member-1");
        order.verify(service).send("room-1", "member-1", "message-1", "hello");
        // capture()는 publish()에 전달된 메시지를 보관한다. 횟수를 생략한 verify()는 한번 호출됐음을 확인한다.
        order.verify(relay).publish(published.capture());
        // verifyNoMoreInteractions()는 마지막으로 확인한 발행 이후에 추가 호출이 없음을 확인한다.
        order.verifyNoMoreInteractions();

        // 저장 결과를 새 객체로 바꾸지 않고 한 번만 처리되어야 한다.
        // getAllValues()는 보관한 인자 전체를 반환하고, containsExactly()는 개수와 순서까지 비교한다.
        assertThat(published.getAllValues()).containsExactly(message);
        // getValue()는 마지막으로 보관한 인자를 반환한다. isSameAs()는 같은 객체임을 확인한다.
        assertThat(published.getValue()).isSameAs(message);
    }

    @Test
    void 미참여자는_저장과_발행을_하지_않는다() {
        // given
        // 참여 확인에서 예외가 발생하도록 설정한다.
        // doThrow(...).when(...)은 반환값이 없는 메서드를 호출했을 때 지정한 예외가 발생하도록 설정한다.
        doThrow(new AccessDeniedException("join first"))
                .when(access).checkParticipant("room-1", "member-1");

        // when / then
        // 메시지를 전송하면 예외가 발생해야 한다.
        assertThatThrownBy(() -> controller.send("room-1", command, () -> "member-1"))
                .isInstanceOf(AccessDeniedException.class);

        // 참여 확인에 실패했으므로 저장과 발행은 실행되지 않아야 한다.
        // verifyNoInteractions()는 service와 relay의 메서드가 한번도 호출되지 않았음을 확인한다.
        verifyNoInteractions(service, relay);
    }

    @Test
    void 저장이_실패하면_발행하지_않는다() {
        // given
        // when(...).thenThrow(...)는 send()가 호출되면 저장 실패 예외가 발생하도록 설정한다.
        when(service.send("room-1", "member-1", "message-1", "hello"))
                .thenThrow(new IllegalStateException("db"));

        // when / then
        // 메시지 전송 요청에 저장 실패 예외가 전달되어야 한다.
        assertThatThrownBy(() -> controller.send("room-1", command, () -> "member-1"))
                .isInstanceOf(IllegalStateException.class);

        // 저장하지 못한 메시지는 Relay에 발행하지 않아야 한다.
        // verifyNoInteractions()는 relay의 메서드가 한번도 호출되지 않았음을 확인한다.
        verifyNoInteractions(relay);
    }
}
