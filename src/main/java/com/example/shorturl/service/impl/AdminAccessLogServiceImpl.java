package com.example.shorturl.service.impl;

import com.example.shorturl.common.PageResult;
import com.example.shorturl.dto.AdminListAccessLogDTO;
import com.example.shorturl.entity.AccessLogDO;
import com.example.shorturl.mapper.AccessLogMapper;
import com.example.shorturl.service.AdminAccessLogService;
import com.example.shorturl.vo.AdminAccessLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccessLogServiceImpl implements AdminAccessLogService {

    private final AccessLogMapper accessLogMapper;

    @Override
    public PageResult<AdminAccessLogVO> listAccessLogs(AdminListAccessLogDTO dto) {
        LocalDateTime start = dto.getStartDate() != null
                ? dto.getStartDate().atStartOfDay() : null;
        LocalDateTime end = dto.getEndDate() != null
                ? dto.getEndDate().plusDays(1).atStartOfDay() : null;

        int offset = (dto.getPage() - 1) * dto.getSize();

        List<AccessLogDO> records = accessLogMapper.selectPageByShortCode(
                dto.getShortCode(), start, end, offset, dto.getSize());
        long total = accessLogMapper.countByShortCode(dto.getShortCode(), start, end);

        List<AdminAccessLogVO> vos = records.stream().map(this::toVO).toList();
        return PageResult.of(vos, total, dto.getPage(), dto.getSize());
    }

    private AdminAccessLogVO toVO(AccessLogDO record) {
        AdminAccessLogVO vo = new AdminAccessLogVO();
        vo.setId(record.getId());
        vo.setShortCode(record.getShortCode());
        vo.setClientIp(record.getIp());
        vo.setUserAgent(record.getUserAgent());
        vo.setReferer(record.getReferer());
        vo.setAccessedAt(record.getAccessedAt());
        return vo;
    }
}
