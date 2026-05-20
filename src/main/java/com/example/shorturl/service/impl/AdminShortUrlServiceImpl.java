package com.example.shorturl.service.impl;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.common.PageResult;
import com.example.shorturl.dto.AdminListShortUrlDTO;
import com.example.shorturl.entity.ShortUrlDO;
import com.example.shorturl.exception.BizException;
import com.example.shorturl.mapper.ShortUrlMapper;
import com.example.shorturl.service.AdminShortUrlService;
import com.example.shorturl.service.ShortUrlCacheService;
import com.example.shorturl.vo.AdminShortUrlVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShortUrlServiceImpl implements AdminShortUrlService {

    private final ShortUrlMapper shortUrlMapper;
    private final ShortUrlCacheService cacheService;

    @Value("${shorturl.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public PageResult<AdminShortUrlVO> listShortUrls(AdminListShortUrlDTO dto) {
        String keyword = blankToNull(dto.getKeyword());
        int offset = (dto.getPage() - 1) * dto.getSize();

        List<AdminShortUrlVO> records = shortUrlMapper.selectAdminPage(keyword, offset, dto.getSize());
        long total = shortUrlMapper.countAdmin(keyword);

        records.forEach(vo -> vo.setShortUrl(baseUrl + "/" + vo.getShortCode()));
        return PageResult.of(records, total, dto.getPage(), dto.getSize());
    }

    @Override
    public AdminShortUrlVO getById(Long id) {
        ShortUrlDO record = shortUrlMapper.selectByPrimaryKey(id);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "short url not found: " + id);
        }
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        ShortUrlDO record = shortUrlMapper.selectByPrimaryKey(id);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "short url not found: " + id);
        }
        shortUrlMapper.deleteByPrimaryKey(id);
        cacheService.evict(record.getShortCode());
        log.info("admin deleted short url, id={}, shortCode={}", id, record.getShortCode());
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AdminShortUrlVO toVO(ShortUrlDO record) {
        AdminShortUrlVO vo = new AdminShortUrlVO();
        vo.setId(record.getId());
        vo.setShortCode(record.getShortCode());
        vo.setShortUrl(baseUrl + "/" + record.getShortCode());
        vo.setOriginalUrl(record.getOriginalUrl());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
