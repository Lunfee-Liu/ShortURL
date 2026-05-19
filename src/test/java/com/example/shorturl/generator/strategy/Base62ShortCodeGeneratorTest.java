package com.example.shorturl.generator.strategy;

import com.example.shorturl.util.Base62;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base62ShortCodeGeneratorTest {

    private final Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator();

    @Test
    void generateFromId() {
        assertThat(generator.generateFromId(1L)).isEqualTo("1");
        assertThat(generator.generateFromId(62L)).isEqualTo("10");
        assertThat(generator.generateFromId(3844L)).isEqualTo("100");
    }

    @Test
    void generateFromIdZero() {
        assertThat(generator.generateFromId(0L)).isEqualTo("0");
    }

    @Test
    void generateFromIdRoundTripMatchesBase62() {
        long id = 123456789L;
        assertThat(generator.generateFromId(id)).isEqualTo(Base62.encode(id));
    }

    @Test
    void generateReturnsNonEmpty() {
        String result = generator.generate("https://example.com");
        assertThat(result).isNotBlank();
    }

    @Test
    void generateDeterministic() {
        String url = "https://example.com/path";
        assertThat(generator.generate(url)).isEqualTo(generator.generate(url));
    }

    @Test
    void generatePlaceholderStartsWithTilde() {
        String placeholder = generator.generatePlaceholder("https://example.com");
        assertThat(placeholder).startsWith("~");
    }

    @Test
    void generatePlaceholderDifferentUrls() {
        String p1 = generator.generatePlaceholder("https://example.com/a");
        String p2 = generator.generatePlaceholder("https://example.com/b");
        assertThat(p1).isNotEqualTo(p2);
    }
}
