package com.example.shorturl.generator.strategy;

import com.example.shorturl.util.Base62;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62ShortCodeGeneratorTest {

    private static final String STD = Base62ShortCodeGenerator.DEFAULT_ALPHABET;
    private final Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator(STD, 6);

    @Test
    void generateFromId_paddedToMinLength() {
        // ID=1 → encode="1" → padded to 6 with STD[0]='0'
        assertThat(generator.generateFromId(1L)).isEqualTo("000001");
    }

    @Test
    void generateFromId_62_paddedToMinLength() {
        assertThat(generator.generateFromId(62L)).isEqualTo("000010");
    }

    @Test
    void generateFromId_largeId_nopadding() {
        // ID=62^6 → naturally 7 chars, no padding
        long id = (long) Math.pow(62, 6);
        assertThat(generator.generateFromId(id).length()).isEqualTo(7);
    }

    @Test
    void generateFromId_zero() {
        assertThat(generator.generateFromId(0L)).isEqualTo("000000");
    }

    @Test
    void generateFromId_roundTripMatchesBase62WithPadding() {
        long id = 123456789L;
        // standard encode of 123456789 = 5 chars, min=6, so 1 pad char '0'
        String expected = "0" + Base62.encode(id);
        assertThat(generator.generateFromId(id)).isEqualTo(expected);
    }

    @Test
    void generateFromId_shuffledAlphabet_differentFromStandard() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        assertThat(shuffledGen.generateFromId(15L)).isNotEqualTo(generator.generateFromId(15L));
    }

    @Test
    void generateFromId_shuffledAlphabet_padCharIsFirstChar() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        // ID=1 → encode="6" (shuffled[1]='6') → pad to 6 with 'n' (shuffled[0])
        assertThat(shuffledGen.generateFromId(1L)).isEqualTo("nnnnn6");
    }

    @Test
    void generatePlaceholderStartsWithTilde() {
        assertThat(generator.generatePlaceholder("https://example.com")).startsWith("~");
    }

    @Test
    void generatePlaceholderDifferentUrls() {
        assertThat(generator.generatePlaceholder("https://a.com"))
                .isNotEqualTo(generator.generatePlaceholder("https://b.com"));
    }

    @Test
    void generateReturnsNonEmpty() {
        assertThat(generator.generate("https://example.com")).isNotBlank();
    }

    @Test
    void constructor_invalidAlphabetLength_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("tooshort", 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("62");
    }

    @Test
    void constructor_duplicateCharsInAlphabet_throws() {
        String dup = "a".repeat(62);
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(dup, 6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthNine_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
