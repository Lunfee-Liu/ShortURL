package com.example.shorturl.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ACCESS_LOG_TOPIC = "access-log";

    @Bean
    public NewTopic accessLogTopic() {
        return TopicBuilder.name(ACCESS_LOG_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
