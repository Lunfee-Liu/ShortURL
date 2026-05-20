package com.example.shorturl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogDO {
    private Long id;
    private String shortCode;
    private String ip;
    private String userAgent;
    private String referer;
    private LocalDateTime accessedAt;
    private LocalDateTime createdAt;
}
