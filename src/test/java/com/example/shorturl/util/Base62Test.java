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
}
