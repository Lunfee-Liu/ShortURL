package com.example.shorturl.service.impl;

import com.example.shorturl.config.KafkaConfig;
import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.service.AccessLogRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAccessLogRecorder implements AccessLogRecorder {

    private final KafkaTemplate<String, AccessLogBO> kafkaTemplate;

    @Override
    public void recordAccess(String shortCode, String originalUrl,
                             String clientIp, String userAgent, String referer) {
        var bo = new AccessLogBO(shortCode, originalUrl, clientIp, userAgent, referer, LocalDateTime.now());
        kafkaTemplate.send(KafkaConfig.ACCESS_LOG_TOPIC, shortCode, bo)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send access log to Kafka, shortCode={}", shortCode, ex);
                    }
                });
    }
}
