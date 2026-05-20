package com.example.shorturl.controller;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.exception.BizException;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShortUrlController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                MybatisAutoConfiguration.class,
                KafkaAutoConfiguration.class
        })
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortUrlService shortUrlService;

    @Test
    void createShortUrlSuccess() throws Exception {
        ShortUrlVO vo = new ShortUrlVO();
        vo.setShortCode("abc123");
        vo.setShortUrl("http://localhost:8080/abc123");
        vo.setOriginalUrl("https://example.com");
        when(shortUrlService.createShortUrl(any(CreateShortUrlDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.shortCode").value("abc123"))
                .andExpect(jsonPath("$.data.shortUrl").value("http://localhost:8080/abc123"))
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com"));
    }

    @Test
    void createShortUrlEmptyUrl() throws Exception {
        mockMvc.perform(post("/api/v1/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()));
    }

    @Test
    void createShortUrlMissingUrl() throws Exception {
        mockMvc.perform(post("/api/v1/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()));
    }

    @Test
    void createShortUrlInvalidScheme() throws Exception {
        mockMvc.perform(post("/api/v1/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()));
    }

    @Test
    void createShortUrlServiceError() throws Exception {
        when(shortUrlService.createShortUrl(any(CreateShortUrlDTO.class)))
                .thenThrow(new BizException(ErrorCode.INTERNAL_ERROR, "service error"));

        mockMvc.perform(post("/api/v1/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()));
    }
}
