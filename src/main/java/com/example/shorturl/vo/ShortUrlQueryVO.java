package com.example.shorturl.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortUrlQueryVO {

    private String shortCode;

    private String originalUrl;
}
