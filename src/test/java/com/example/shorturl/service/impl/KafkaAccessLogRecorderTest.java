package com.example.shorturl.service.impl;

import com.example.shorturl.config.KafkaConfig;
import com.example.shorturl.dto.AccessLogBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAccessLogRecorderTest {

    @Mock
    private KafkaTemplate<String, AccessLogBO> kafkaTemplate;

    @InjectMocks
    private KafkaAccessLogRecorder recorder;

    @Test
    void recordAccess_sendsMessageToKafkaWithCorrectFields() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        recorder.recordAccess("abc123", "https://example.com", "1.2.3.4", "curl/8.0", null);

        ArgumentCaptor<AccessLogBO> captor = ArgumentCaptor.forClass(AccessLogBO.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.ACCESS_LOG_TOPIC), eq("abc123"), captor.capture());

        AccessLogBO sent = captor.getValue();
        assertThat(sent.shortCode()).isEqualTo("abc123");
        assertThat(sent.originalUrl()).isEqualTo("https://example.com");
        assertThat(sent.clientIp()).isEqualTo("1.2.3.4");
        assertThat(sent.userAgent()).isEqualTo("curl/8.0");
        assertThat(sent.referer()).isNull();
        assertThat(sent.accessedAt()).isNotNull();
    }

    @Test
    void recordAccess_doesNotThrowWhenKafkaFails() {
        CompletableFuture<SendResult<String, AccessLogBO>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        assertThatCode(() ->
                recorder.recordAccess("abc123", "https://example.com", "1.2.3.4", "curl/8.0", null)
        ).doesNotThrowAnyException();
    }
}
