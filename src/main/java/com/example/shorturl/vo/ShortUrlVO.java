package com.example.shorturl.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortUrlVO {

    private String shortCode;

    private String shortUrl;

    private String originalUrl;
}
