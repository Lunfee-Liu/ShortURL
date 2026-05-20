package com.example.shorturl.generator.strategy;

import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.util.Base62;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    public static final String DEFAULT_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static final int MAX_CODE_LENGTH = 8;
    private static final int PLACEHOLDER_HASH_LIMIT = 100_000;

    private final String alphabet;
    private final int minCodeLength;
    private final long scrambleKey;
    private final long codeOffset;  // = 62^(minCodeLength-1)
    private final long rangeSize;   // = codeOffset * 61，乘法置换的值域大小

    public Base62ShortCodeGenerator(
            @Value("${shorturl.generator.alphabet:" + DEFAULT_ALPHABET + "}") String alphabet,
            @Value("${shorturl.generator.min-code-length:6}") int minCodeLength,
            @Value("${shorturl.generator.scramble-key:2654435761}") long scrambleKey) {

        if (alphabet.length() != 62 || alphabet.chars().distinct().count() != 62) {
            throw new IllegalArgumentException(
                    "shorturl.generator.alphabet must be exactly 62 unique characters");
        }
        if (minCodeLength < 1 || minCodeLength > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "shorturl.generator.min-code-length must be between 1 and " + MAX_CODE_LENGTH);
        }
        if (scrambleKey <= 0) {
            throw new IllegalArgumentException(
                    "shorturl.generator.scramble-key must be positive");
        }

        long offset = 1L;
        for (int i = 0; i < minCodeLength - 1; i++) {
            offset *= 62;
        }
        long range = offset * 61;

        if (!BigInteger.valueOf(scrambleKey).gcd(BigInteger.valueOf(range)).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException(
                    "shorturl.generator.scramble-key must be coprime with range size " + range);
        }

        this.alphabet = alphabet;
        this.minCodeLength = minCodeLength;
        this.scrambleKey = scrambleKey;
        this.codeOffset = offset;
        this.rangeSize = range;
    }

    @Override
    public String generate(String originalUrl) {
        return Base62.encode(hashUrl(originalUrl), alphabet);
    }

    @Override
    public String generateFromId(Long id) {
        long scrambled = (id * scrambleKey) % rangeSize;
        String code = Base62.encode(scrambled + codeOffset, alphabet);
        assert code.length() <= MAX_CODE_LENGTH : "short code overflow for id=" + id;
        return code;
    }

    @Override
    public String generatePlaceholder(String originalUrl) {
        long hash = hashUrl(originalUrl) ^ System.nanoTime();
        return "~" + Base62.encode(Math.abs(hash) % PLACEHOLDER_HASH_LIMIT, alphabet);
    }

    private static long hashUrl(String url) {
        long h = 0;
        for (int i = 0; i < url.length(); i++) {
            h = 31 * h + url.charAt(i);
        }
        return Math.abs(h);
    }
}
