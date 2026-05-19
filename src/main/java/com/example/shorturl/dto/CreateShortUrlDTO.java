package com.example.shorturl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateShortUrlDTO {

    @NotBlank(message = "url must not be blank")
    @Size(max = 2048, message = "url must not exceed 2048 characters")
    @Pattern(regexp = "^(https?)://[\\w\\-./?#@!$&'()*+,;%=:]+$",
            message = "url must be a valid http or https URL")
    private String url;
}
