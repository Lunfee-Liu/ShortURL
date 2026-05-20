package com.example.shorturl.aspect;

import com.example.shorturl.service.AccessLogRecorder;
import com.example.shorturl.util.IpUtil;
import com.example.shorturl.vo.ShortUrlQueryVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AccessLogAspect {

    private final AccessLogRecorder accessLogRecorder;
    private final HttpServletRequest request;

    @AfterReturning(
            pointcut = "@annotation(com.example.shorturl.annotation.RecordAccessLog)",
            returning = "result"
    )
    public void recordAccess(JoinPoint joinPoint, ShortUrlQueryVO result) {
        String shortCode = (String) joinPoint.getArgs()[0];

        accessLogRecorder.recordAccess(
                shortCode,
                result.getOriginalUrl(),
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );
    }
}
