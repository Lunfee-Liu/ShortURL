package com.example.shorturl.service.impl;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.service.ShortUrlCacheService;
import com.example.shorturl.dto.CreateShortUrlDTO;
import com.example.shorturl.entity.ShortUrlDO;
import com.example.shorturl.exception.BizException;
import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.generator.strategy.Base62ShortCodeGenerator;
import com.example.shorturl.mapper.ShortUrlMapper;
import com.example.shorturl.service.ShortUrlService;
import com.example.shorturl.vo.ShortUrlQueryVO;
import com.example.shorturl.vo.ShortUrlVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    @Mock
    private ShortUrlMapper shortUrlMapper;

    @Mock
    private ShortUrlCacheService cacheService;

    private final ShortCodeGenerator shortCodeGenerator =
            new Base62ShortCodeGenerator(Base62ShortCodeGenerator.DEFAULT_ALPHABET, 6);
    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        shortUrlService = new ShortUrlServiceImpl(shortUrlMapper, shortCodeGenerator, cacheService);
        ReflectionTestUtils.setField(shortUrlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createShortUrl() {
        // Capture the placeholder short code at insert time (before service modifies the object)
        AtomicReference<String> capturedPlaceholder = new AtomicReference<>();
        doAnswer(invocation -> {
            ShortUrlDO record = invocation.getArgument(0);
            capturedPlaceholder.set(record.getShortCode());
            record.setId(1L);
            return 1;
        }).when(shortUrlMapper).insert(any(ShortUrlDO.class));

        CreateShortUrlDTO dto = new CreateShortUrlDTO();
        dto.setUrl("https://example.com/path");

        ShortUrlVO vo = shortUrlService.createShortUrl(dto);

        assertThat(vo.getShortCode()).isEqualTo("000001");
        assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/000001");
        assertThat(vo.getOriginalUrl()).isEqualTo("https://example.com/path");

        // Verify placeholder starts with ~
        assertThat(capturedPlaceholder.get()).startsWith("~");

        // Verify insert was called
        verify(shortUrlMapper).insert(any(ShortUrlDO.class));

        // Verify update was called with real short code
        verify(shortUrlMapper).updateByPrimaryKey(argThat(record ->
                "000001".equals(record.getShortCode()) && record.getId() == 1L));
    }

    @Test
    void getOriginalUrlCacheHit() {
        when(cacheService.getOriginalUrl("abc123")).thenReturn("https://example.com");

        ShortUrlQueryVO vo = shortUrlService.getOriginalUrl("abc123");

        assertThat(vo.getShortCode()).isEqualTo("abc123");
        assertThat(vo.getOriginalUrl()).isEqualTo("https://example.com");

        // No DB query on cache hit
        verify(shortUrlMapper, never()).selectByShortCode(anyString());
    }

    @Test
    void getOriginalUrlCacheMissDbHit() {
        when(cacheService.getOriginalUrl("abc123")).thenReturn(null);

        ShortUrlDO record = new ShortUrlDO();
        record.setId(1L);
        record.setShortCode("abc123");
        record.setOriginalUrl("https://example.com");
        when(shortUrlMapper.selectByShortCode("abc123")).thenReturn(record);

        ShortUrlQueryVO vo = shortUrlService.getOriginalUrl("abc123");

        assertThat(vo.getOriginalUrl()).isEqualTo("https://example.com");

        // Cache populated after DB hit
        verify(cacheService).cacheShortUrl("abc123", "https://example.com");
    }

    @Test
    void getOriginalUrlCacheMissDbMiss() {
        when(cacheService.getOriginalUrl("missing")).thenReturn(null);
        when(shortUrlMapper.selectByShortCode("missing")).thenReturn(null);

        assertThatThrownBy(() -> shortUrlService.getOriginalUrl("missing"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

        // Empty marker cached
        verify(cacheService).cacheEmpty("missing");
    }

    @Test
    void getOriginalUrlEmptyMarker() {
        when(cacheService.getOriginalUrl("blocked")).thenReturn(ShortUrlCacheService.EMPTY_MARKER);

        assertThatThrownBy(() -> shortUrlService.getOriginalUrl("blocked"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

        verify(shortUrlMapper, never()).selectByShortCode(anyString());
    }

    @Test
    void createShortUrlBase62EncodingForLargerIds() {
        doAnswer(invocation -> {
            ShortUrlDO record = invocation.getArgument(0);
            record.setId(62L);
            return 1;
        }).when(shortUrlMapper).insert(any(ShortUrlDO.class));

        CreateShortUrlDTO dto = new CreateShortUrlDTO();
        dto.setUrl("https://example.com/62");

        ShortUrlVO vo = shortUrlService.createShortUrl(dto);

        assertThat(vo.getShortCode()).isEqualTo("000010");
    }
}
