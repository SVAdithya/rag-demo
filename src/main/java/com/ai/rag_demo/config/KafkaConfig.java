package com.ai.rag_demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String DOCUMENT_UPLOAD_TOPIC = "document-upload";
    public static final String DOCUMENT_PROCESSING_TOPIC = "document-processing";
    public static final String CHAT_REQUEST_TOPIC = "chat-request";

    @Bean
    public NewTopic documentUploadTopic() {
        return TopicBuilder.name(DOCUMENT_UPLOAD_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentProcessingTopic() {
        return TopicBuilder.name(DOCUMENT_PROCESSING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatRequestTopic() {
        return TopicBuilder.name(CHAT_REQUEST_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
