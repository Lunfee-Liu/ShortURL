package com.example.shorturl.service;

import com.example.shorturl.common.PageResult;
import com.example.shorturl.dto.AdminListAccessLogDTO;
import com.example.shorturl.vo.AdminAccessLogVO;

public interface AdminAccessLogService {

    PageResult<AdminAccessLogVO> listAccessLogs(AdminListAccessLogDTO dto);
}
