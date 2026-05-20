package com.example.shorturl.event;

public record ShortUrlCreatedEvent(String shortCode, String originalUrl) {}
