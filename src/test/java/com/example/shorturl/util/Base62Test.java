package com.example.shorturl.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

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
