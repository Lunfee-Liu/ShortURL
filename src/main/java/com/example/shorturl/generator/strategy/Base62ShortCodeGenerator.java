package com.example.shorturl.generator.strategy;

import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.util.Base62;
import org.springframework.stereotype.Component;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    private static final int PLACEHOLDER_HASH_LIMIT = 100_000;

    @Override
    public String generate(String originalUrl) {
        long hash = hashUrl(originalUrl);
        return Base62.encode(hash);
    }

    public String generateFromId(Long id) {
        return Base62.encode(id);
    }

    public String generatePlaceholder(String originalUrl) {
        long hash = hashUrl(originalUrl) ^ System.nanoTime();
        return "~" + Base62.encode(Math.abs(hash) % PLACEHOLDER_HASH_LIMIT);
    }

    private static long hashUrl(String url) {
        long h = 0;
        for (int i = 0; i < url.length(); i++) {
            h = 31 * h + url.charAt(i);
        }
        return Math.abs(h);
    }
}
