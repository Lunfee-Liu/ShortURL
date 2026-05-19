package com.example.shorturl.service;

public interface AccessLogRecorder {

    void recordAccess(String shortCode, String originalUrl,
                      String clientIp, String userAgent, String referer);
}
