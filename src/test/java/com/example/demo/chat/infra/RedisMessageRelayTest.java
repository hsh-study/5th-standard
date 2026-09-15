package com.example.demo.chat.infra;

import com.example.demo.chat.domain.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RedisMessageRelayTest {

    @Test
    void 발행_JSON을_다른_Adapter에서_복원해_모든_로컬_구독자에게_전달한다() throws Exception {
        // given
        // 메시지를 보내는 서버와 받는 서버에서 사용할 Relay, 수신 결과를 담을 목록 두 개를 준비한다.
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        // mock()으로 실제 Redis에 접속하지 않고 메서드 호출과 전달된 인자를 확인할 객체를 만든다.
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);

        RedisMessageRelay sender = new RedisMessageRelay(template, json, container);
        RedisMessageRelay receiver = new RedisMessageRelay(template, json, container);
        List<ChatMessage> first = new ArrayList<>();
        List<ChatMessage> second = new ArrayList<>();

        // when
        // 받는 서버의 Relay에 Redis Listener와 메시지를 받으면 실행할 함수 두 개를 등록한다.
        receiver.registerListener();
        receiver.subscribe(first::add);
        receiver.subscribe(second::add);

        // then
        // chat.messages 채널에 Listener가 등록됐음을 확인한다.
        // ArgumentCaptor는 메서드에 전달된 인자를 보관한다. Listener와 채널을 각각 가져온다.
        ArgumentCaptor<MessageListener> listener = ArgumentCaptor.forClass(MessageListener.class);
        ArgumentCaptor<ChannelTopic> topic = ArgumentCaptor.forClass(ChannelTopic.class);
        // verify()는 한번 호출됐음을 확인하고, capture()는 그때 전달된 인자를 보관한다.
        verify(container).addMessageListener(listener.capture(), topic.capture());

        // getValue()로 보관한 채널과 Listener를 꺼내 확인한다.
        assertThat(topic.getValue().getTopic()).isEqualTo("chat.messages");
        assertThat(listener.getValue()).isNotNull();

        // given
        // 식별자와 시간이 정해진 한글 메시지를 준비한다.
        ChatMessage message = ChatMessage.create(
                UUID.randomUUID(), "room-1", 42L,
                "member-1", "message-1", "한글 메시지",
                Instant.parse("2026-09-07T00:00:00Z")
        );

        // when
        // 보내는 서버의 Relay가 메시지를 Redis 채널에 발행한다.
        sender.publish(message);

        // then
        // 발행 채널과 JSON 문자열을 확인한다.
        ArgumentCaptor<String> channel = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        // convertAndSend()가 한번 호출됐음을 확인하고 발행 채널과 JSON을 보관한다.
        verify(template).convertAndSend(channel.capture(), payload.capture());

        assertThat(channel.getValue()).isEqualTo("chat.messages");
        assertThat(payload.getValue()).isNotBlank();

        // given
        // 발행한 JSON을 UTF-8 바이트로 전달해 Listener의 복원 과정을 확인한다.
        // mock()으로 Redis가 전달하는 메시지를 대신할 객체를 만든다.
        Message wireMessage = mock(Message.class);
        // when(...).thenReturn(...)은 getBody()가 호출되면 준비한 JSON 바이트를 반환하도록 설정한다.
        when(wireMessage.getBody()).thenReturn(payload.getValue().getBytes(StandardCharsets.UTF_8));

        // when
        // 받는 서버의 Listener가 Redis에서 메시지를 받은 상황을 재현한다.
        listener.getValue().onMessage(wireMessage, null);

        // then
        // 받는 서버에 등록한 두 함수에 같은 메시지가 한 건씩 전달됐음을 확인한다.
        assertThat(first).hasSize(1);
        assertThat(second).containsExactly(first.get(0));

        // 객체를 다시 JSON으로 비교해 식별자·본문·시간 등 모든 필드가 보존됐음을 확인한다.
        JsonNode restored = json.valueToTree(first.get(0));
        JsonNode original = json.valueToTree(message);

        assertThat(restored).isEqualTo(original);
        assertThat(first.get(0).getId()).isEqualTo(42L);
        assertThat(first.get(0).getMessageId()).isEqualTo(message.getMessageId());
        assertThat(json.readTree(payload.getValue()).properties()).hasSize(7);
    }

    @Test
    void 잘못된_JSON은_전달하지_않고_원인을_남긴다() {
        // given
        // 수신된 메시지를 기록할 함수와 Relay를 준비한다.
        // mock()으로 실제 Redis에 접속하지 않고 메서드 호출과 전달된 인자를 확인할 객체를 만든다.
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        RedisMessageRelay relay = new RedisMessageRelay(template, json, container);
        List<ChatMessage> received = new ArrayList<>();

        relay.subscribe(received::add);

        // when / then
        // 잘못된 JSON을 받으면 역직렬화 실패 예외가 발생해야 한다.
        assertThatThrownBy(() -> relay.onRedisMessage("broken-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 메시지 역직렬화 실패");

        // 복원하지 못한 메시지는 수신 함수에 전달하지 않아야 한다.
        assertThat(received).isEmpty();
    }
}
