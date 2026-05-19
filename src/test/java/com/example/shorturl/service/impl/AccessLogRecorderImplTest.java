package com.example.shorturl.service.impl;

import com.example.shorturl.service.AccessLogRecorder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class AccessLogRecorderImplTest {

    private final AccessLogRecorder recorder = new AccessLogRecorderImpl();

    @Test
    void recordAccessWithoutQueryString() {
        assertThatCode(() -> recorder.recordAccess(
                "abc123",
                "https://example.com/path",
                "192.168.1.1",
                "curl/8.0",
                "https://referer.com"
        )).doesNotThrowAnyException();
    }

    @Test
    void recordAccessWithQueryString() {
        assertThatCode(() -> recorder.recordAccess(
                "abc123",
                "https://example.com/path?token=secret&key=value",
                "10.0.0.1",
                "Mozilla/5.0",
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void recordAccessWithNullFields() {
        assertThatCode(() -> recorder.recordAccess(
                "abc123",
                null,
                null,
                null,
                null
        )).doesNotThrowAnyException();
    }
}
