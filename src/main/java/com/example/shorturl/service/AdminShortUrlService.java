package com.example.shorturl.service;

import com.example.shorturl.common.PageResult;
import com.example.shorturl.dto.AdminListShortUrlDTO;
import com.example.shorturl.vo.AdminShortUrlVO;

public interface AdminShortUrlService {

    PageResult<AdminShortUrlVO> listShortUrls(AdminListShortUrlDTO dto);

    AdminShortUrlVO getById(Long id);

    void deleteById(Long id);
}
