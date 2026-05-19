package com.example.shorturl.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpUtilTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void xForwardedForSingleIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        assertThat(IpUtil.getClientIp(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void xForwardedForChain() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1, 172.16.0.1");
        assertThat(IpUtil.getClientIp(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void xRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        assertThat(IpUtil.getClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void fallbackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        assertThat(IpUtil.getClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void skipUnknownValue() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        assertThat(IpUtil.getClientIp(request)).isEqualTo("10.0.0.1");
    }
}
