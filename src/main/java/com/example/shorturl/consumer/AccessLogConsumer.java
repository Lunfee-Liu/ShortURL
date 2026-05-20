package com.example.shorturl.consumer;

import com.example.shorturl.config.KafkaConfig;
import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.service.AccessLogPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogConsumer {

    private final AccessLogPersistenceService persistenceService;

    @KafkaListener(topics = KafkaConfig.ACCESS_LOG_TOPIC, concurrency = "3")
    public void consume(List<AccessLogBO> records, Acknowledgment ack) {
        if (records.isEmpty()) {
            ack.acknowledge();
            return;
        }
        try {
            persistenceService.batchPersist(records);
            ack.acknowledge();
            log.debug("Consumed and persisted {} access logs", records.size());
        } catch (Exception e) {
            log.error("Failed to persist access logs batch, count={}, firstCode={}",
                    records.size(), records.get(0).shortCode(), e);
            throw e;
        }
    }
}
