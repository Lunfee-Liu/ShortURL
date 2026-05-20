package com.example.shorturl.service.impl;

import com.example.shorturl.event.ShortUrlCreatedEvent;
import com.example.shorturl.service.ShortUrlCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortUrlCacheEventListenerTest {

    @Mock
    private ShortUrlCacheService cacheService;

    @InjectMocks
    private ShortUrlCacheEventListener listener;

    @Test
    void onShortUrlCreated_delegatesToCacheService() {
        listener.onShortUrlCreated(new ShortUrlCreatedEvent("3tdk01", "https://example.com"));

        verify(cacheService).cacheShortUrl("3tdk01", "https://example.com");
    }
}
