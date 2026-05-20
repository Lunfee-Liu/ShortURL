package com.example.shorturl.consumer;

import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.entity.AccessLogDO;
import com.example.shorturl.mapper.AccessLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLogConsumerTest {

    @Mock
    private AccessLogMapper accessLogMapper;

    @InjectMocks
    private AccessLogConsumer consumer;

    @Test
    void consume_batchInsertsMappedLogs() {
        LocalDateTime now = LocalDateTime.now();
        List<AccessLogBO> bos = List.of(
                new AccessLogBO("abc123", "https://x.com", "1.2.3.4", "curl/8", null, now),
                new AccessLogBO("def456", "https://y.com", "5.6.7.8", "Mozilla/5", "https://ref.com", now)
        );

        consumer.consume(bos);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccessLogDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(accessLogMapper).batchInsert(captor.capture());

        List<AccessLogDO> inserted = captor.getValue();
        assertThat(inserted).hasSize(2);
        assertThat(inserted.get(0).getShortCode()).isEqualTo("abc123");
        assertThat(inserted.get(0).getIp()).isEqualTo("1.2.3.4");
        assertThat(inserted.get(1).getShortCode()).isEqualTo("def456");
        assertThat(inserted.get(1).getReferer()).isEqualTo("https://ref.com");
        assertThat(inserted.get(0).getAccessedAt()).isEqualTo(now);
    }

    @Test
    void consume_emptyListSkipsInsert() {
        consumer.consume(List.of());
        verify(accessLogMapper, never()).batchInsert(anyList());
    }

    @Test
    void consume_rethrowsOnInsertFailureToTriggerKafkaRetry() {
        doThrow(new RuntimeException("DB error")).when(accessLogMapper).batchInsert(anyList());
        List<AccessLogBO> bos = List.of(
                new AccessLogBO("abc123", "https://x.com", "1.2.3.4", "curl/8", null, LocalDateTime.now())
        );

        assertThatThrownBy(() -> consumer.consume(bos))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }
}
