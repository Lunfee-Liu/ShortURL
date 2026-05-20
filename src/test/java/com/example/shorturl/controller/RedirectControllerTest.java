package com.example.shorturl.controller;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.exception.BizException;
import com.example.shorturl.service.AccessLogRecorder;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlQueryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RedirectController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        MybatisAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShortUrlService shortUrlService;

    @MockBean
    private AccessLogRecorder accessLogRecorder;

    @Test
    void redirectSuccess() throws Exception {
        ShortUrlQueryVO vo = new ShortUrlQueryVO();
        vo.setShortCode("abc123");
        vo.setOriginalUrl("https://example.com");
        when(shortUrlService.getOriginalUrl("abc123")).thenReturn(vo);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void redirectNotFound() throws Exception {
        when(shortUrlService.getOriginalUrl(anyString()))
                .thenThrow(new BizException(ErrorCode.NOT_FOUND, "short code not found: missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void redirectWithInvalidShortCode() throws Exception {
        // More than 8 characters should not match
        mockMvc.perform(get("/too-long-short-code"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectApiPathNotCaptured() throws Exception {
        // /api/v1/short-url should not match the redirect pattern
        mockMvc.perform(get("/api/v1/short-url"))
                .andExpect(status().isNotFound());
    }
}
