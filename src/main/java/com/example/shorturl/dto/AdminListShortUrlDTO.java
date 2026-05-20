package com.example.shorturl.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminListShortUrlDTO {

    /** keyword searches short_code or original_url, nullable = no filter */
    private String keyword;

    @Min(value = 1, message = "page must be >= 1")
    private int page = 1;

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    private int size = 20;
}
