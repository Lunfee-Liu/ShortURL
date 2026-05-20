package com.example.shorturl.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminAccessLogVO {

    private Long id;
    private String shortCode;
    private String clientIp;
    private String userAgent;
    private String referer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime accessedAt;
}
