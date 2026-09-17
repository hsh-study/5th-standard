package com.example.demo.chat.infra;

import com.example.demo.chat.application.event.InspectionPublisher;
import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.config.KafkaInspectionConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaInspectionPublisher implements InspectionPublisher {
    private static final Logger log =
        LoggerFactory.getLogger(KafkaInspectionPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @Override
    public void publish(ChatMessageRecorded event) {

        try {
            // 객체 직렬화
            String json = mapper.writeValueAsString(event);

            kafka.send(KafkaInspectionConfig.TOPIC, event.roomId(), json)
                .whenComplete((result, error) -> {

                    if (error != null) {

                        log.error("메시지 발행 실패 eventId={}", event.eventId(), error);

                    } else {

                        log.info("메시지 발행 eventId={} partition={} offset={}",
                            event.eventId(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }

                });

        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("메시지 직렬화 실패 : ", ex);
        }
    }
}
