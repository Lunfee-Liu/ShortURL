package com.example.shorturl.vo;

import lombok.Data;

@Data
public class ShortUrlVO {

    private String shortCode;

    private String shortUrl;

    private String originalUrl;
}
