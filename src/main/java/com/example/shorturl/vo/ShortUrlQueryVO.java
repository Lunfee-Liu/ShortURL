package com.example.shorturl.vo;

import lombok.Data;

@Data
public class ShortUrlQueryVO {

    private String shortCode;

    private String originalUrl;
}
