package com.example.shorturl.service.impl;

import com.example.shorturl.dto.AccessLogBO;
import com.example.shorturl.entity.AccessLogDO;
import com.example.shorturl.mapper.AccessLogMapper;
import com.example.shorturl.service.AccessLogPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogPersistenceServiceImpl implements AccessLogPersistenceService {

    private final AccessLogMapper accessLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchPersist(List<AccessLogBO> records) {
        List<AccessLogDO> logs = records.stream()
                .map(bo -> AccessLogDO.builder()
                        .shortCode(bo.shortCode())
                        .originalUrl(bo.originalUrl())
                        .ip(bo.clientIp())
                        .userAgent(bo.userAgent())
                        .referer(bo.referer())
                        .accessedAt(bo.accessedAt())
                        .build())
                .toList();
        accessLogMapper.batchInsert(logs);
        log.debug("Batch persisted {} access logs", logs.size());
    }
}
