package com.example.shorturl.service;

import com.example.shorturl.vo.KafkaStatsVO;
import com.example.shorturl.vo.RedisStatsVO;

public interface MonitorService {

    RedisStatsVO getRedisStats();

    KafkaStatsVO getKafkaStats();
}
