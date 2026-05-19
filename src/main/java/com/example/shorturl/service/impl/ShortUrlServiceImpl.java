package com.example.shorturl.service.impl;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.config.ShortUrlCacheService;
import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.entity.ShortUrlDO;
import com.example.shorturl.exception.BizException;
import com.example.shorturl.generator.strategy.Base62ShortCodeGenerator;
import com.example.shorturl.mapper.ShortUrlMapper;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlQueryVO;
import com.example.shorturl.vo.ShortUrlVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlMapper shortUrlMapper;
    private final Base62ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlCacheService cacheService;

    @Value("${shorturl.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ShortUrlVO createShortUrl(CreateShortUrlDTO dto) {
        String originalUrl = dto.getUrl();

        // Phase 1: insert with placeholder short code
        ShortUrlDO record = new ShortUrlDO();
        record.setOriginalUrl(originalUrl);
        record.setShortCode(shortCodeGenerator.generatePlaceholder(originalUrl));
        record.setIsDeleted(false);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        shortUrlMapper.insert(record);

        // Phase 2: generate real short code from auto-increment ID
        String shortCode = shortCodeGenerator.generateFromId(record.getId());
        record.setShortCode(shortCode);
        shortUrlMapper.updateByPrimaryKey(record);

        // Phase 3: write to Redis cache after transaction commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheService.cacheShortUrl(shortCode, originalUrl);
                }
            });
        }

        // Phase 4: build VO
        ShortUrlVO vo = new ShortUrlVO();
        vo.setShortCode(shortCode);
        vo.setShortUrl(baseUrl + "/" + shortCode);
        vo.setOriginalUrl(originalUrl);
        return vo;
    }

    @Override
    public ShortUrlQueryVO getOriginalUrl(String shortCode) {
        // Cache-aside pattern
        String cached = cacheService.getOriginalUrl(shortCode);

        if (cached != null) {
            if (ShortUrlCacheService.EMPTY_MARKER.equals(cached)) {
                throw new BizException(ErrorCode.NOT_FOUND, "short code not found: " + shortCode);
            }
            return buildQueryVO(shortCode, cached);
        }

        // Cache miss: query DB
        ShortUrlDO record = shortUrlMapper.selectByShortCode(shortCode);
        if (record == null) {
            cacheService.cacheEmpty(shortCode);
            throw new BizException(ErrorCode.NOT_FOUND, "short code not found: " + shortCode);
        }

        cacheService.cacheShortUrl(shortCode, record.getOriginalUrl());
        return buildQueryVO(shortCode, record.getOriginalUrl());
    }

    private static ShortUrlQueryVO buildQueryVO(String shortCode, String originalUrl) {
        ShortUrlQueryVO vo = new ShortUrlQueryVO();
        vo.setShortCode(shortCode);
        vo.setOriginalUrl(originalUrl);
        return vo;
    }
}
