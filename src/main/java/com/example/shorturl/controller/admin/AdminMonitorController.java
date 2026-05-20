package com.example.shorturl.controller.admin;

import com.example.shorturl.common.Result;
import com.example.shorturl.service.MonitorService;
import com.example.shorturl.vo.KafkaStatsVO;
import com.example.shorturl.vo.RedisStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final MonitorService monitorService;

    @GetMapping("/redis")
    public Result<RedisStatsVO> redis() {
        return Result.success(monitorService.getRedisStats());
    }

    @GetMapping("/kafka")
    public Result<KafkaStatsVO> kafka() {
        return Result.success(monitorService.getKafkaStats());
    }
}
