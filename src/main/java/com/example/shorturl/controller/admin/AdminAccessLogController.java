package com.example.shorturl.controller.admin;

import com.example.shorturl.common.PageResult;
import com.example.shorturl.common.Result;
import com.example.shorturl.dto.AdminListAccessLogDTO;
import com.example.shorturl.service.AdminAccessLogService;
import com.example.shorturl.vo.AdminAccessLogVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/access-logs")
@RequiredArgsConstructor
public class AdminAccessLogController {

    private final AdminAccessLogService adminAccessLogService;

    @GetMapping
    public Result<PageResult<AdminAccessLogVO>> list(@Valid AdminListAccessLogDTO dto) {
        return Result.success(adminAccessLogService.listAccessLogs(dto));
    }
}
