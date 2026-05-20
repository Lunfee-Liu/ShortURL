package com.example.shorturl.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Base62 {

    public static String encode(long value, String alphabet) {
        if (alphabet.length() != 62) {
            throw new IllegalArgumentException("alphabet must be exactly 62 characters, got: " + alphabet.length());
        }
        if (value == 0) {
            return String.valueOf(alphabet.charAt(0));
        }
        char[] alpha = alphabet.toCharArray();
        StringBuilder sb = new StringBuilder();
        long n = value;
        while (n > 0) {
            sb.append(alpha[(int) (n % 62)]);
            n /= 62;
        }
        return sb.reverse().toString();
    }

}
