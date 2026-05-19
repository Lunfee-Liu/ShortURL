package com.example.shorturl.config;

public interface ShortUrlCacheService {

    String EMPTY_MARKER = "__EMPTY__";

    void cacheShortUrl(String shortCode, String originalUrl);

    void cacheEmpty(String shortCode);

    String getOriginalUrl(String shortCode);

    void evict(String shortCode);
}
