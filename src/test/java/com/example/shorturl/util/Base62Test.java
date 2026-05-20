package com.example.shorturl.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    void encodeZero() {
        assertThat(Base62.encode(0)).isEqualTo("0");
    }

    @Test
    void encodeSmallNumber() {
        assertThat(Base62.encode(125)).isEqualTo("21");
    }

    @Test
    void encodeBaseBoundary() {
        assertThat(Base62.encode(62)).isEqualTo("10");
        assertThat(Base62.encode(3844)).isEqualTo("100");
    }

    @Test
    void decodeZero() {
        assertThat(Base62.decode("0")).isZero();
    }

    @Test
    void decodeSmallString() {
        assertThat(Base62.decode("21")).isEqualTo(125);
    }

    @Test
    void decodeBaseBoundary() {
        assertThat(Base62.decode("10")).isEqualTo(62);
        assertThat(Base62.decode("100")).isEqualTo(3844);
    }

    @Test
    void roundTrip() {
        long[] values = {0, 1, 61, 62, 125, 1000, 99999, 1000000, Long.MAX_VALUE};
        for (long value : values) {
            String encoded = Base62.encode(value);
            long decoded = Base62.decode(encoded);
            assertThat(decoded).isEqualTo(value);
        }
    }

    @Test
    void rejectEmptyInput() {
        assertThatThrownBy(() -> Base62.decode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectInvalidCharacter() {
        assertThatThrownBy(() -> Base62.decode("hello!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid character");
    }

    @Test
    void encodeWithCustomAlphabet_singleDigit() {
        String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        // 1 % 62 = 1 → alphabet[1] = '6'
        assertThat(Base62.encode(1L, alphabet)).isEqualTo("6");
    }

    @Test
    void encodeWithCustomAlphabet_zero() {
        String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        // 0 → first char of alphabet = 'n'
        assertThat(Base62.encode(0L, alphabet)).isEqualTo("n");
    }

    @Test
    void encodeWithCustomAlphabet_multiDigit() {
        String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        // 62: 62%62=0→'n', 62/62=1, 1%62=1→'6', reversed="6n"
        assertThat(Base62.encode(62L, alphabet)).isEqualTo("6n");
    }

    @Test
    void encodeWithCustomAlphabet_wrongLength_throws() {
        assertThatThrownBy(() -> Base62.encode(1L, "tooshort"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("62");
    }
}
