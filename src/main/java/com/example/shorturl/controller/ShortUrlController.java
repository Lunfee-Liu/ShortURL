package com.example.shorturl.controller;

import com.example.shorturl.common.Result;
import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/short-url")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @PostMapping
    public Result<ShortUrlVO> createShortUrl(@Valid @RequestBody CreateShortUrlDTO dto) {
        ShortUrlVO vo = shortUrlService.createShortUrl(dto);
        return Result.success(vo);
    }
}
