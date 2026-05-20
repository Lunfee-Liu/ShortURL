package com.example.shorturl.service.impl;

import com.example.shorturl.config.KafkaConfig;
import com.example.shorturl.service.MonitorService;
import com.example.shorturl.vo.KafkaStatsVO;
import com.example.shorturl.vo.RedisStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private static final String KEY_PATTERN = "shorturl:url:*";
    private static final int KEY_SCAN_LIMIT = 200;
    private static final String CONSUMER_GROUP = "access-log-recorder";

    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaAdmin kafkaAdmin;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Redis ────────────────────────────────────────────────────────────────

    @Override
    public RedisStatsVO getRedisStats() {
        RedisStatsVO vo = new RedisStatsVO();

        // 1. INFO stats: keyspace_hits / keyspace_misses
        try {
            Properties info = stringRedisTemplate.execute(
                    (org.springframework.data.redis.core.RedisCallback<Properties>)
                    conn -> conn.serverCommands().info("stats"));
            if (info != null) {
                long hits   = parseLong(info.getProperty("keyspace_hits",   "0"));
                long misses = parseLong(info.getProperty("keyspace_misses", "0"));
                vo.setKeyspaceHits(hits);
                vo.setKeyspaceMisses(misses);
                long total = hits + misses;
                vo.setHitRate(total == 0 ? -1.0 : (double) hits / total);
            }
        } catch (Exception e) {
            log.warn("failed to read Redis INFO stats", e);
        }

        // 2. SCAN shorturl:url:* — capped at KEY_SCAN_LIMIT
        List<RedisStatsVO.CacheKeyVO> keys = new ArrayList<>();
        try {
            ScanOptions opts = ScanOptions.scanOptions()
                    .match(KEY_PATTERN).count(KEY_SCAN_LIMIT).build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(opts)) {
                while (cursor.hasNext() && keys.size() < KEY_SCAN_LIMIT) {
                    String key = cursor.next();
                    Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                    RedisStatsVO.CacheKeyVO kvo = new RedisStatsVO.CacheKeyVO();
                    kvo.setKey(key);
                    kvo.setTtlSeconds(ttl != null ? ttl : -2L);
                    keys.add(kvo);
                }
            }
        } catch (Exception e) {
            log.warn("failed to scan Redis keys", e);
        }

        vo.setKeyCount(keys.size());
        vo.setKeys(keys);
        return vo;
    }

    // ── Kafka ────────────────────────────────────────────────────────────────

    @Override
    public KafkaStatsVO getKafkaStats() {
        KafkaStatsVO vo = new KafkaStatsVO();
        vo.setTopic(KafkaConfig.ACCESS_LOG_TOPIC);
        vo.setConsumerGroup(CONSUMER_GROUP);

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // a. end offsets (producer side)
            int partitions = 3;
            Map<TopicPartition, OffsetSpec> endOffsetRequest = new HashMap<>();
            for (int p = 0; p < partitions; p++) {
                endOffsetRequest.put(
                        new TopicPartition(KafkaConfig.ACCESS_LOG_TOPIC, p),
                        OffsetSpec.latest());
            }
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    adminClient.listOffsets(endOffsetRequest).all().get(5, TimeUnit.SECONDS);

            // b. committed offsets (consumer side)
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committed =
                    adminClient.listConsumerGroupOffsets(CONSUMER_GROUP)
                            .partitionsToOffsetAndMetadata()
                            .get(5, TimeUnit.SECONDS);

            List<KafkaStatsVO.PartitionStatsVO> partitionList = new ArrayList<>();
            long totalLag = 0;
            for (int p = 0; p < partitions; p++) {
                var tp = new TopicPartition(KafkaConfig.ACCESS_LOG_TOPIC, p);
                long endOffset = endOffsets.getOrDefault(tp,
                        new ListOffsetsResult.ListOffsetsResultInfo(0, -1, Optional.empty())).offset();
                var committedMeta = committed.get(tp);
                long committedOffset = committedMeta != null ? committedMeta.offset() : 0L;
                long lag = Math.max(0, endOffset - committedOffset);
                totalLag += lag;

                KafkaStatsVO.PartitionStatsVO pvo = new KafkaStatsVO.PartitionStatsVO();
                pvo.setPartition(p);
                pvo.setEndOffset(endOffset);
                pvo.setCommittedOffset(committedOffset);
                pvo.setLag(lag);
                partitionList.add(pvo);
            }
            vo.setPartitions(partitionList);
            vo.setTotalLag(totalLag);
        } catch (Exception e) {
            log.warn("failed to fetch Kafka stats", e);
            vo.setPartitions(List.of());
        }
        return vo;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }
}
