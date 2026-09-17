package com.example.demo.chat.application.event;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.config.KafkaInspectionConfig;
import com.example.demo.chat.domain.ChatMessage;
import com.example.demo.chat.domain.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChatInspectionReplayCommand implements ApplicationRunner {

    private final ChatMessageRepository messages;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @Value("${kafka.inspection.relay-message-id}")
    private UUID messageId;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        ChatMessage message = messages.findByMessageId(messageId)
            .orElseThrow(() -> new IllegalArgumentException("원본 메시지가 없습니다: " + messageId));

        ChatMessageRecorded recorded = ChatMessageRecorded.from(message);

        // Kafka 재발행
        RecordMetadata metadata = kafka.send(
                KafkaInspectionConfig.TOPIC,
                recorded.roomId(),
                mapper.writeValueAsString(recorded))
            .get(15, TimeUnit.SECONDS)
            .getRecordMetadata();

        LoggerFactory.getLogger(ChatInspectionReplayCommand.class)
            .info("재발행 완료 eventId={} partition={} offset={}",
                recorded.eventId(), metadata.partition(), metadata.offset());

    }
}
