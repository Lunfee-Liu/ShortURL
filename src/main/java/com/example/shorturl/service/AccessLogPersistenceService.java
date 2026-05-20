package com.example.shorturl.service;

import com.example.shorturl.dto.AccessLogBO;

import java.util.List;

public interface AccessLogPersistenceService {
    void batchPersist(List<AccessLogBO> records);
}
