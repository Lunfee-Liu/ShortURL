package com.example.shorturl.generator.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62ShortCodeGeneratorTest {

    private static final String STD = Base62ShortCodeGenerator.DEFAULT_ALPHABET;
    private static final long   KEY = 2_654_435_761L;
    private final Base62ShortCodeGenerator generator =
            new Base62ShortCodeGenerator(STD, 6, KEY);

    // ── 核心：连续 ID 产出无关联短码 ──────────────────────────────────

    @Test
    void generateFromId_id1_expectedValue() {
        // scrambled = 1 × 2_654_435_761 % rangeSize = 2_654_435_761
        // encoded   = 2_654_435_761 + 916_132_832 = 3_570_568_593 → "3tdk01"
        assertThat(generator.generateFromId(1L)).isEqualTo("3tdk01");
    }

    @Test
    void generateFromId_id2_noRelationToId1() {
        // encoded = 5_308_871_522 + 916_132_832 = 6_225_004_354 → "6nHU02"
        assertThat(generator.generateFromId(2L)).isEqualTo("6nHU02");
        assertThat(generator.generateFromId(2L).charAt(0))
                .isNotEqualTo(generator.generateFromId(1L).charAt(0));
    }

    @Test
    void generateFromId_allSmallIds_alwaysSixChars() {
        for (long id = 0; id <= 1_000; id++) {
            assertThat(generator.generateFromId(id).length())
                    .as("ID=%d must produce exactly 6 chars", id)
                    .isEqualTo(6);
        }
    }

    @Test
    void generateFromId_allSmallIds_noCollisions() {
        long[] ids = {0, 1, 2, 3, 100, 1000, 62, 3844};
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (long id : ids) {
            codes.add(generator.generateFromId(id));
        }
        assertThat(codes).hasSize(ids.length);
    }

    @Test
    void generateFromId_deterministic() {
        assertThat(generator.generateFromId(42L)).isEqualTo(generator.generateFromId(42L));
    }

    @Test
    void generateFromId_id0() {
        // scrambled = 0, encoded = 916_132_832 → "100000"
        assertThat(generator.generateFromId(0L)).isEqualTo("100000");
    }

    @Test
    void generateFromId_largeId_validLength() {
        long bigId = 62L * 62 * 62 * 62 * 62 * 62; // 62^6
        String code = generator.generateFromId(bigId);
        assertThat(code.length()).isBetween(6, 8);
    }

    // ── 乱序字母表：首字符不是重复补位字符 ────────────────────────────

    @Test
    void generateFromId_shuffledAlphabet_noLeadingRepeat() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator sg = new Base62ShortCodeGenerator(shuffled, 6, KEY);
        String code = sg.generateFromId(1L);
        assertThat(code).hasSize(6);
        assertThat(code).doesNotMatch("^(.)\\1{4}.*");
    }

    @Test
    void generateFromId_differentAlphabets_differentCodes() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator sg = new Base62ShortCodeGenerator(shuffled, 6, KEY);
        assertThat(sg.generateFromId(1L)).isNotEqualTo(generator.generateFromId(1L));
    }

    // ── placeholder & generate ─────────────────────────────────────────

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

    // ── 构造器校验 ──────────────────────────────────────────────────────

    @Test
    void constructor_invalidAlphabetLength_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("tooshort", 6, KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("62");
    }

    @Test
    void constructor_duplicateCharsInAlphabet_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("a".repeat(62), 6, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 0, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthNine_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 9, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_scrambleKeyNotCoprimeWithRange_throws() {
        // key=2 is even → gcd(2, rangeSize) = 2 ≠ 1
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 6, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coprime");
    }

    @Test
    void constructor_scrambleKeyZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 6, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
