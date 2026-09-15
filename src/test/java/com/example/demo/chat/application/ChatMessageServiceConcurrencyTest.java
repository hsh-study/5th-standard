package com.example.demo.chat.application;

import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
public class ChatMessageServiceConcurrencyTest {

    @Autowired
    private ChatMessageService service;

    @MockitoSpyBean
    private ChatMessageRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 같은_요청이_동시에_들어와도_같은_원본을_반환한다() throws InterruptedException, ExecutionException, TimeoutException {

        String roomId = "room-" + UUID.randomUUID();
        String clientMessageId = "client-" + UUID.randomUUID();

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger queryCount = new AtomicInteger();

        doAnswer(invocation -> {
            // 실제 DB 조회
            Optional<ChatMessage> chatMessage = entityManager.createQuery(
                """
                            select m from ChatMessage m
                            where m.roomId = :roomId
                            and m.clientMessageId = :clientMessageId
                        """, ChatMessage.class)
                .setParameter("roomId", roomId)
                .setParameter("clientMessageId", clientMessageId)
                .getResultList()
                .stream()
                .findFirst();

            // 두 요청의 조회 queryCount <= 2 동안은
            // barrier 를 대기시켜 INSERT 를 경쟁상태로 만들기 위함이다.
            if (queryCount.incrementAndGet() <= 2) {
                barrier.await(5, TimeUnit.SECONDS);
            }

            return chatMessage;
        }).when(repository).findByRoomIdAndClientMessageId(roomId, clientMessageId);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ChatMessage> request1 = executor.submit(() ->
                service.send(roomId, "member-1", clientMessageId, "content-1")
            );

            Future<ChatMessage> request2 = executor.submit(() ->
                service.send(roomId, "member-1", clientMessageId, "content-1")
            );

            // Future 작업이 완료될 때까지 기다린 뒤 service.send 가 반환한 메시지를 받음
            // 10초안에 메시지를 받지 못하면 TimeoutException 발생
            ChatMessage message1 = request1.get(10, TimeUnit.SECONDS);
            ChatMessage message2 = request2.get(10, TimeUnit.SECONDS);

            // 두 요청이 같은 메시지를 반환하는지 확인
            assertThat(message1.getMessageId())
                .isEqualTo(message2.getMessageId());

            // 요청은 두번이지만 하나의 메시지만 저장되었는지 확인
            Long count = entityManager.createQuery(
                """
                        select count(m) from ChatMessage m
                        where m.roomId = :roomId
                            and m.clientMessageId = :clientMessageId
                        """, Long.class
                )
                .setParameter("roomId", roomId)
                .setParameter("clientMessageId", clientMessageId)
                .getSingleResult();

            // 저장된 메시지가 하나만 존재하는지 확인
            assertThat(count).isEqualTo(1L);

        } finally {
            executor.shutdown();

            if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                repository.deleteAll(
                    repository.findByRoomIdAndClientMessageId(
                        roomId, clientMessageId
                    ).stream().toList()
                );
            }
        }
    }
}
