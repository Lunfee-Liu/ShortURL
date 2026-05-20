package com.example.shorturl.service.impl;

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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLogPersistenceServiceImplTest {

    @Mock
    private AccessLogMapper accessLogMapper;

    @InjectMocks
    private AccessLogPersistenceServiceImpl service;

    @Test
    void batchPersist_mapsBoToDoAndCallsMapper() {
        LocalDateTime now = LocalDateTime.now();
        List<AccessLogBO> bos = List.of(
                new AccessLogBO("abc123", "https://x.com", "1.2.3.4", "curl/8", null, now),
                new AccessLogBO("def456", "https://y.com", "5.6.7.8", "Mozilla/5", "https://ref.com", now)
        );

        service.batchPersist(bos);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccessLogDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(accessLogMapper).batchInsert(captor.capture());

        List<AccessLogDO> inserted = captor.getValue();
        assertThat(inserted).hasSize(2);
        assertThat(inserted.get(0).getShortCode()).isEqualTo("abc123");
        assertThat(inserted.get(0).getOriginalUrl()).isEqualTo("https://x.com");
        assertThat(inserted.get(0).getIp()).isEqualTo("1.2.3.4");
        assertThat(inserted.get(0).getUserAgent()).isEqualTo("curl/8");
        assertThat(inserted.get(0).getReferer()).isNull();
        assertThat(inserted.get(0).getAccessedAt()).isEqualTo(now);
        assertThat(inserted.get(1).getShortCode()).isEqualTo("def456");
        assertThat(inserted.get(1).getOriginalUrl()).isEqualTo("https://y.com");
        assertThat(inserted.get(1).getReferer()).isEqualTo("https://ref.com");
    }
}
