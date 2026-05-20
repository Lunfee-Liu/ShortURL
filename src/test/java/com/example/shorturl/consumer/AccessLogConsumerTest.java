package com.example.shorturl.consumer;

import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.service.AccessLogPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLogConsumerTest {

    @Mock
    private AccessLogPersistenceService persistenceService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private AccessLogConsumer consumer;

    @Test
    void consume_persistsAndAcknowledges() {
        List<AccessLogBO> bos = List.of(
                new AccessLogBO("abc123", "https://x.com", "1.2.3.4", "curl/8", null, LocalDateTime.now())
        );

        consumer.consume(bos, acknowledgment);

        verify(persistenceService).batchPersist(bos);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_emptyList_acknowledgesWithoutPersisting() {
        consumer.consume(List.of(), acknowledgment);

        verify(persistenceService, never()).batchPersist(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_persistenceFailure_doesNotAcknowledge_andRethrows() {
        doThrow(new RuntimeException("DB error")).when(persistenceService).batchPersist(any());
        List<AccessLogBO> bos = List.of(
                new AccessLogBO("abc123", "https://x.com", "1.2.3.4", "curl/8", null, LocalDateTime.now())
        );

        assertThatThrownBy(() -> consumer.consume(bos, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");

        verify(acknowledgment, never()).acknowledge();
    }
}
