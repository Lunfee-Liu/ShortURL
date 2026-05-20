package com.example.shorturl.service.impl;

import com.example.shorturl.event.ShortUrlCreatedEvent;
import com.example.shorturl.service.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortUrlCacheEventListener {

    private final ShortUrlCacheService cacheService;

    @TransactionalEventListener
    public void onShortUrlCreated(ShortUrlCreatedEvent event) {
        cacheService.cacheShortUrl(event.shortCode(), event.originalUrl());
        log.debug("cached short url after commit: {}", event.shortCode());
    }
}
