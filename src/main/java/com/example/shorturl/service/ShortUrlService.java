package com.example.shorturl.service;

import com.example.shorturl.annotation.RecordAccessLog;
import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.vo.ShortUrlQueryVO;
import com.example.shorturl.vo.ShortUrlVO;

public interface ShortUrlService {

    ShortUrlVO createShortUrl(CreateShortUrlDTO dto);

    @RecordAccessLog
    ShortUrlQueryVO getOriginalUrl(String shortCode);
}
