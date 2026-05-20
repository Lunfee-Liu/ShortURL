package com.example.shorturl.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KafkaStatsVO {

    private String topic;
    private String consumerGroup;

    /** Total lag across all partitions */
    private long totalLag;

    private List<PartitionStatsVO> partitions;

    @Getter
    @Setter
    public static class PartitionStatsVO {
        private int partition;
        /** Latest produced offset (log end) */
        private long endOffset;
        /** Latest committed offset by consumer group */
        private long committedOffset;
        /** endOffset - committedOffset */
        private long lag;
    }
}
