package com.example.shorturl.controller;

import com.example.shorturl.service.AccessLogRecorder;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.util.IpUtil;
import com.example.shorturl.vo.ShortUrlQueryVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlService shortUrlService;
    private final AccessLogRecorder accessLogRecorder;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{1,8}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest request) {
        ShortUrlQueryVO query = shortUrlService.getOriginalUrl(shortCode);

        accessLogRecorder.recordAccess(
                shortCode,
                query.getOriginalUrl(),
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(query.getOriginalUrl()))
                .build();
    }
}
