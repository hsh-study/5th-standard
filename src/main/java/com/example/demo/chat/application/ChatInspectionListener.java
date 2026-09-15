package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.config.KafkaInspectionConfig;
import com.example.demo.chat.domain.ChatInspectionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatInspectionListener {

    private static final Logger log = LoggerFactory.getLogger(ChatInspectionListener.class);

    private final ObjectMapper mapper;
    private final ChatInspectionService service;

    @KafkaListener(
        id = KafkaInspectionConfig.LISTENER,
        groupId = "${kafka.inspection.group-id:chat-inspection}",
        topics = KafkaInspectionConfig.TOPIC,
        containerFactory = "inspectionFactory",
        autoStartup = "${kafka.inspection.listener-auto-start:true}")
    public void receive(String json, Acknowledgment ack) throws Exception {

        ChatMessageRecorded event = mapper.readValue(json, ChatMessageRecorded.class);
        ChatInspectionResult result = service.inspect(event);

        log.info("검사 결과 저장 eventId={} verdict={}", event.eventId(), result.getVerdict());

        ack.acknowledge();
    }

}
