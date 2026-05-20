package com.example.shorturl.generator.strategy;

import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.util.Base62;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    public static final String DEFAULT_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static final int MAX_CODE_LENGTH = 8;
    private static final int PLACEHOLDER_HASH_LIMIT = 100_000;

    private final String alphabet;
    private final int minCodeLength;

    public Base62ShortCodeGenerator(
            @Value("${shorturl.generator.alphabet:" + DEFAULT_ALPHABET + "}") String alphabet,
            @Value("${shorturl.generator.min-code-length:6}") int minCodeLength) {
        if (alphabet.length() != 62 || alphabet.chars().distinct().count() != 62) {
            throw new IllegalArgumentException(
                    "shorturl.generator.alphabet must be exactly 62 unique characters");
        }
        if (minCodeLength < 1 || minCodeLength > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "shorturl.generator.min-code-length must be between 1 and " + MAX_CODE_LENGTH);
        }
        this.alphabet = alphabet;
        this.minCodeLength = minCodeLength;
    }

    @Override
    public String generate(String originalUrl) {
        return Base62.encode(hashUrl(originalUrl), alphabet);
    }

    @Override
    public String generateFromId(Long id) {
        String code = Base62.encode(id, alphabet);
        assert code.length() <= MAX_CODE_LENGTH : "short code overflow for id=" + id;
        String pad = String.valueOf(alphabet.charAt(0));
        return pad.repeat(Math.max(0, minCodeLength - code.length())) + code;
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
