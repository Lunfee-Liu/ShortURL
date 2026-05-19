package com.example.shorturl.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShortUrlDO {
    private Long id;

    private String shortCode;

    private String originalUrl;

    private String shardKey;

    private Boolean isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}