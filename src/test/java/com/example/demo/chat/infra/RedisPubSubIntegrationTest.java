package com.example.demo.chat.infra;

import com.example.demo.chat.application.ChatRelayFanout;
import com.example.demo.chat.domain.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisPubSubIntegrationTest {

    @Test
    void Redis로_서버에_전파하고_각_Fanout이_Topic에_전달한다() throws Exception {
        // given
        // GenericContainer는 Docker에서 테스트용 Redis를 실행하고 종료할 수 있게 해 준다.
        // withExposedPorts(6379)는 Redis 포트를 호스트의 사용 가능한 포트에 연결한다.
        // try 블록이 끝나면 close()가 호출되어 컨테이너를 정리한다.
        try (GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                .withExposedPorts(6379)) {

            // start()는 Redis 컨테이너를 실행하고 사용할 준비가 될 때까지 기다린다.
            redis.start();

            // CountDownLatch(2)는 A와 B 서버가 모두 메시지를 받을 때까지 기다리는 데 사용한다.
            // CopyOnWriteArrayList는 각 서버의 수신 결과를 다른 스레드에서도 안전하게 읽을 수 있게 한다.
            CountDownLatch delivered = new CountDownLatch(2);
            List<StompDelivery> receivedA = new CopyOnWriteArrayList<>();
            List<StompDelivery> receivedB = new CopyOnWriteArrayList<>();
            SimpMessagingTemplate stompA = recordingTemplate(delivered, receivedA);
            SimpMessagingTemplate stompB = recordingTemplate(delivered, receivedB);

            // 두 서버가 같은 Redis 채널을 구독하도록 초기화한다.
            try (AnnotationConfigApplicationContext a = node(redis, stompA);
                 AnnotationConfigApplicationContext b = node(redis, stompB)) {

                // 메시지를 발행하기 전에 두 서버가 시작됐는지 확인한다.
                assertThat(a.getBean(RedisMessageListenerContainer.class).isRunning()).isTrue();
                assertThat(b.getBean(RedisMessageListenerContainer.class).isRunning()).isTrue();

                ChatMessage message = ChatMessage.create(
                        UUID.randomUUID(), "room-1", 42L,
                        "member-1", "message-1", "한글 Redis 통신",
                        Instant.parse("2026-09-11T00:00:00Z")
                );

                // when
                // A 서버에서 메시지를 한 번 발행한다.
                a.getBean(RedisMessageRelay.class).publish(message);

                // then
                // await(10, TimeUnit.SECONDS)는 두 서버의 메시지 수신을 최대 10초 동안 기다린다.
                // 두 서버가 메시지를 받아 countDown()이 두 번 호출되면 true, 시간이 지나면 false를 반환한다.
                assertThat(delivered.await(10, TimeUnit.SECONDS))
                        .as("두 연결의 메시지 수신")
                        .isTrue();

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                JsonNode expected = mapper.valueToTree(message);

                // 각 서버에 한 건씩 전달됐음을 확인한다.
                assertThat(receivedA).hasSize(1);
                assertThat(receivedB).hasSize(1);

                StompDelivery deliveryA = receivedA.get(0);
                StompDelivery deliveryB = receivedB.get(0);
                JsonNode messageA = mapper.valueToTree(deliveryA.message());
                JsonNode messageB = mapper.valueToTree(deliveryB.message());

                // A와 B 서버 모두 같은 방 Topic에 원래 메시지가 전달됐음을 확인한다.
                assertThat(deliveryA.destination()).isEqualTo("/topic/live-sales/room-1");
                assertThat(deliveryB.destination()).isEqualTo("/topic/live-sales/room-1");
                assertThat(messageA).isEqualTo(expected);
                assertThat(messageB).isEqualTo(expected);
            }
        }
    }

    // 각 서버에서 STOMP로 전달한 목적지와 메시지를 기록할 객체를 만든다.
    private SimpMessagingTemplate recordingTemplate(
            CountDownLatch latch,
            List<StompDelivery> received
    ) {
        // mock()으로 브라우저에 실제 전송하지 않고 전달된 목적지와 메시지를 기록할 객체를 만든다.
        SimpMessagingTemplate stomp = mock(SimpMessagingTemplate.class);

        // doAnswer()는 서버의 Fanout이 convertAndSend()를 호출하면 목적지와 메시지를 기록하도록 설정한다.
        // anyString()과 any(ChatMessage.class)는 목적지 문자열과 메시지 값에 관계없이 적용한다.
        doAnswer(call -> {
            // getArgument(0)은 첫 번째 인자인 목적지, getArgument(1)은 두 번째 인자인 메시지를 가져온다.
            received.add(new StompDelivery(call.getArgument(0), call.getArgument(1)));
            // countDown()은 해당 서버의 메시지 수신을 알린다. 두 번 호출되면 await()에서 기다리는 테스트가 계속 실행된다.
            latch.countDown();
            // convertAndSend()는 반환값이 없는 void 메서드이므로 null을 반환한다.
            return null;
        }).when(stomp).convertAndSend(anyString(), any(ChatMessage.class));

        return stomp;
    }

    // A와 B 서버의 동작을 각각 별도의 Spring Context로 재현한다.
    private AnnotationConfigApplicationContext node(
            GenericContainer<?> redis,
            SimpMessagingTemplate stomp
    ) {
        // 각 서버에서 사용할 Redis 연결과 Listener, Relay, Fanout을 등록할 Spring Context를 만든다.
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // registerBean()은 Bean을 만들 방법을 등록한다. getBean()은 등록된 Bean을 가져온다.
        // getMappedPort(6379)는 호스트에서 Redis에 접속할 때 사용할 포트를 반환한다.
        context.registerBean(
                LettuceConnectionFactory.class,
                () -> new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379))
        );
        context.registerBean(
                StringRedisTemplate.class,
                () -> new StringRedisTemplate(context.getBean(LettuceConnectionFactory.class))
        );
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());
        context.registerBean(RedisMessageListenerContainer.class, () -> {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(context.getBean(LettuceConnectionFactory.class));
            return container;
        });
        context.registerBean(SimpMessagingTemplate.class, () -> stomp);
        context.register(RedisMessageRelay.class, ChatRelayFanout.class);

        try {
            // refresh()는 등록한 Bean을 생성하고 초기화 메서드를 실행해 Listener 구독을 시작한다.
            context.refresh();
            return context;
        } catch (RuntimeException | Error exception) {
            // 서버 구성 중 초기화에 실패하면 생성한 Redis 연결을 정리한다.
            context.close();
            throw exception;
        }
    }

    private record StompDelivery(String destination, ChatMessage message) {
    }
}
