package com.example.shorturl.service.impl;

import com.example.shorturl.service.AccessLogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccessLogRecorderImpl implements AccessLogRecorder {

    private static final String LOG_FORMAT = "ACCESS_LOG|shortCode={}|originalUrl={}|ip={}|ua={}|referer={}";

    @Override
    public void recordAccess(String shortCode, String originalUrl,
                             String clientIp, String userAgent, String referer) {
        log.info(LOG_FORMAT, shortCode, sanitizeUrl(originalUrl), clientIp,
                sanitizeUserAgent(userAgent), referer);
    }

    private static String sanitizeUrl(String url) {
        if (url != null && url.contains("?")) {
            return url.substring(0, url.indexOf("?")) + "?...";
        }
        return url;
    }

    private static String sanitizeUserAgent(String ua) {
        return ua != null ? ua : "";
    }
}
