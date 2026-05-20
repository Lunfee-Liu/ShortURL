package com.example.shorturl.consumer;

import com.example.shorturl.config.KafkaConfig;
import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.entity.AccessLogDO;
import com.example.shorturl.mapper.AccessLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogConsumer {

    private final AccessLogMapper accessLogMapper;

    @KafkaListener(topics = KafkaConfig.ACCESS_LOG_TOPIC)
    public void consume(List<AccessLogBO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<AccessLogDO> logs = records.stream()
                .map(bo -> AccessLogDO.builder()
                        .shortCode(bo.shortCode())
                        .ip(bo.clientIp())
                        .userAgent(bo.userAgent())
                        .referer(bo.referer())
                        .accessedAt(bo.accessedAt())
                        .build())
                .toList();
        try {
            accessLogMapper.batchInsert(logs);
            log.debug("Batch inserted {} access logs", logs.size());
        } catch (Exception e) {
            log.error("Failed to batch insert access logs, count={}", logs.size(), e);
            throw e;
        }
    }
}
