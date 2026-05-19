package com.example.shorturl.service;

import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.vo.ShortUrlQueryVO;
import com.example.shorturl.vo.ShortUrlVO;

public interface ShortUrlService {

    ShortUrlVO createShortUrl(CreateShortUrlDTO dto);

    ShortUrlQueryVO getOriginalUrl(String shortCode);
}
