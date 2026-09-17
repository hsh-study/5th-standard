package com.example.demo.chat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaInspectionConfig {

    public static final String TOPIC="chat.message-recorded";
    public static final String LISTENER="chat-inspection";

    // Topic 구성 정보
    @Bean
    NewTopic inspectionTopic() {
        return TopicBuilder
            .name(TOPIC)
            .partitions(3)
            .replicas(1) // Partition 복제 수
            .build();
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> inspectionFactory(
        KafkaProperties properties,
        KafkaListenerEndpointRegistry registry) {

        Map<String, Object> props = new HashMap<>(properties.buildConsumerProperties());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(
                (record, exception) -> {
                    LoggerFactory.getLogger(KafkaInspectionConfig.class)
                        .error(
                            "메시지 검사 에러 topic={} partition={} offset={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            exception);

                    MessageListenerContainer container = registry.getListenerContainer(LISTENER);
                    if (container != null) container.stop(() -> {});

                    throw new IllegalStateException(
                        "메시지 검사 중지. 수정 후 재시작 하세요.", exception);
                },
                // 1초 간격으로 2회 재시도, 최초 요청을 포함하면 3회 진행 됨
                new FixedBackOff(1000L, 2L));

        // 오류 처리가 끝나도 자동으로 커밋하지 않음
        errorHandler.setAckAfterHandle(false);

        // 복구 성공해도 자동으로 커밋하지 않음
        errorHandler.setCommitRecovered(false);

        // 메시지 처리를 실패했을때 실행할 코드를 등록하는 메서드
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> {
            LoggerFactory.getLogger(KafkaInspectionConfig.class)
                .warn(
                    "검사 시도 실패 attempt={} partition={} offset={}",
                    deliveryAttempt,
                    record.partition(),
                    record.offset(),
                    exception);
        });

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
