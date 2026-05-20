package com.example.shorturl.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Base62 {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] CHARS = ALPHABET.toCharArray();
    private static final int BASE = 62;

    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long n = value;
        while (n > 0) {
            sb.append(CHARS[(int) (n % BASE)]);
            n /= BASE;
        }
        return sb.reverse().toString();
    }

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

    public static long decode(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("input must not be empty");
        }
        long result = 0;
        for (int i = 0; i < value.length(); i++) {
            int idx = ALPHABET.indexOf(value.charAt(i));
            if (idx < 0) {
                throw new IllegalArgumentException("invalid character: " + value.charAt(i));
            }
            result = result * BASE + idx;
        }
        return result;
    }
}
