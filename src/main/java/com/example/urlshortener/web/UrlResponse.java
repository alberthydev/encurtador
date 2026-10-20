package com.example.urlshortener.web;

import com.example.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record UrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount
) {
    public static UrlResponse from(ShortUrl e, String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new UrlResponse(e.getShortCode(), base + "/" + e.getShortCode(), e.getOriginalUrl(),
                e.getCreatedAt(), e.getExpiresAt(), e.getClickCount());
    }
}
