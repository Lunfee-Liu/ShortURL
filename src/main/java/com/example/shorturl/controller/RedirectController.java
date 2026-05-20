package com.example.shorturl.controller;

import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlQueryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlService shortUrlService;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{1,8}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        ShortUrlQueryVO query = shortUrlService.getOriginalUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(query.getOriginalUrl()))
                .build();
    }
}
