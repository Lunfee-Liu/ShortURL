package com.example.shorturl.generator;

public interface ShortCodeGenerator {

    String generate(String originalUrl);

    String generateFromId(Long id);

    String generatePlaceholder(String originalUrl);
}
