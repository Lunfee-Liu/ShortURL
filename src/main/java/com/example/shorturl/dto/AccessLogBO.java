package com.example.shorturl.dto;

import java.time.LocalDateTime;

public record AccessLogBO(
        String shortCode,
        String originalUrl,
        String clientIp,
        String userAgent,
        String referer,
        LocalDateTime accessedAt
) {}
