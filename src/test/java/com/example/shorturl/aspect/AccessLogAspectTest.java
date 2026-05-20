package com.example.shorturl.aspect;

import com.example.shorturl.service.AccessLogRecorder;
import com.example.shorturl.vo.ShortUrlQueryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessLogAspectTest {

    @Mock
    private AccessLogRecorder accessLogRecorder;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AccessLogAspect aspect;

    @Test
    void recordAccess_extractsFieldsFromJoinPointAndDelegates() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"abc123"});
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getHeader("User-Agent")).thenReturn("curl/8.0");
        when(request.getHeader("Referer")).thenReturn(null);

        ShortUrlQueryVO result = new ShortUrlQueryVO();
        result.setShortCode("abc123");
        result.setOriginalUrl("https://example.com");

        aspect.recordAccess(joinPoint, result);

        verify(accessLogRecorder).recordAccess(
                eq("abc123"),
                eq("https://example.com"),
                eq("1.2.3.4"),
                eq("curl/8.0"),
                isNull()
        );
    }
}
