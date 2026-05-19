package com.example.shorturl.config.impl;

import com.example.shorturl.config.ShortUrlCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlCacheServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Captor
    private ArgumentCaptor<Long> ttlCaptor;

    private ShortUrlCacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService = new ShortUrlCacheServiceImpl(redisTemplate);
    }

    @Test
    void cacheShortUrl() {
        cacheService.cacheShortUrl("abc123", "https://example.com");

        verify(valueOps).set(eq("shorturl:url:abc123"), eq("https://example.com"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void cacheShortUrlTtlWithinRange() {
        cacheService.cacheShortUrl("abc123", "https://example.com");

        verify(valueOps).set(anyString(), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        long ttl = ttlCaptor.getValue();
        assertThat(ttl).isBetween(77760L, 95040L);
    }

    @Test
    void cacheEmpty() {
        cacheService.cacheEmpty("missing");

        verify(valueOps).set("shorturl:url:missing", ShortUrlCacheService.EMPTY_MARKER, 60L, TimeUnit.SECONDS);
    }

    @Test
    void getOriginalUrlHit() {
        when(valueOps.get("shorturl:url:abc123")).thenReturn("https://example.com");

        String result = cacheService.getOriginalUrl("abc123");

        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    void getOriginalUrlMiss() {
        when(valueOps.get("shorturl:url:abc123")).thenReturn(null);

        String result = cacheService.getOriginalUrl("abc123");

        assertThat(result).isNull();
    }

    @Test
    void evict() {
        cacheService.evict("abc123");

        verify(redisTemplate).delete("shorturl:url:abc123");
    }

    @Test
    void redisExceptionDegradesGracefully() {
        when(valueOps.get("shorturl:url:abc123")).thenThrow(new RuntimeException("connection timeout"));

        String result = cacheService.getOriginalUrl("abc123");

        assertThat(result).isNull();
    }
}
