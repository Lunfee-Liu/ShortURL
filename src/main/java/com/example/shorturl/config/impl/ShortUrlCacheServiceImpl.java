package com.example.shorturl.config.impl;

import com.example.shorturl.config.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlCacheServiceImpl implements ShortUrlCacheService {

    private static final String KEY_PREFIX = "shorturl:url:";
    private static final long BASE_TTL = 86400L;
    private static final long EMPTY_TTL = 60L;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void cacheShortUrl(String shortCode, String originalUrl) {
        try {
            String key = buildKey(shortCode);
            stringRedisTemplate.opsForValue().set(key, originalUrl, computeTtlWithJitter(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("failed to cache short url, shortCode={}", shortCode, e);
        }
    }

    @Override
    public void cacheEmpty(String shortCode) {
        try {
            String key = buildKey(shortCode);
            stringRedisTemplate.opsForValue().set(key, EMPTY_MARKER, EMPTY_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("failed to cache empty marker, shortCode={}", shortCode, e);
        }
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        try {
            String key = buildKey(shortCode);
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("failed to get from cache, shortCode={}", shortCode, e);
            return null;
        }
    }

    @Override
    public void evict(String shortCode) {
        try {
            String key = buildKey(shortCode);
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("failed to evict cache, shortCode={}", shortCode, e);
        }
    }

    private static String buildKey(String shortCode) {
        return KEY_PREFIX + shortCode;
    }

    private static long computeTtlWithJitter() {
        long jitter = (long) (BASE_TTL * (Math.random() - 0.5) * 0.2);
        return BASE_TTL + jitter;
    }
}
