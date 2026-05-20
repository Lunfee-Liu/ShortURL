package com.example.shorturl.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RedisStatsVO {

    /** Number of shorturl:url:* keys currently in Redis (capped at 200) */
    private int keyCount;

    /** Cumulative cache hits since Redis started */
    private long keyspaceHits;

    /** Cumulative cache misses since Redis started */
    private long keyspaceMisses;

    /** Hit rate in [0, 1], -1 if no requests yet */
    private double hitRate;

    /** Sampled key entries (at most 200) */
    private List<CacheKeyVO> keys;

    @Getter
    @Setter
    public static class CacheKeyVO {
        /** Full Redis key, e.g. shorturl:url:abc123 */
        private String key;
        /** Remaining TTL in seconds, -1 = no expiry, -2 = key does not exist */
        private long ttlSeconds;
    }
}
