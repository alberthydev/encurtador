package com.example.urlshortener.service;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortUrlExpiredException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.example.urlshortener.web.CreateUrlRequest;
import com.example.urlshortener.web.UrlResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final Set<String> RESERVED = Set.of("api", "actuator", "error");

    private final ShortUrlRepository repository;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();
    private final Counter createdCounter;
    private final Counter redirectCounter;

    public UrlShortenerService(ShortUrlRepository repository,
                               MeterRegistry registry,
                               @Value("${app.base-url}") String baseUrl) {
        this.repository = repository;
        this.baseUrl = baseUrl;
        this.createdCounter = Counter.builder("urlshortener.urls.created")
                .description("Total de URLs encurtadas").register(registry);
        this.redirectCounter = Counter.builder("urlshortener.redirects")
                .description("Total de redirecionamentos realizados").register(registry);
    }

    @Transactional
    public UrlResponse create(CreateUrlRequest request) {
        validateUri(request.url());

        boolean custom = request.customAlias() != null && !request.customAlias().isBlank();
        String code;
        if (custom) {
            code = request.customAlias();
            if (RESERVED.contains(code.toLowerCase()) || repository.existsByShortCode(code)) {
                log.warn("Alias already in use or reserved: {}", code);
                throw new AliasAlreadyExistsException(code);
            }
        } else {
            code = generateUniqueCode();
        }

        Instant now = Instant.now();
        Instant expiresAt = request.expiresInDays() == null ? null : now.plus(request.expiresInDays(), ChronoUnit.DAYS);

        try {
            ShortUrl saved = repository.saveAndFlush(new ShortUrl(code, request.url(), now, expiresAt));
            createdCounter.increment();
            log.info("Short URL created: code={} target={}", code, request.url());
            return UrlResponse.from(saved, baseUrl);
        } catch (DataIntegrityViolationException e) {
            // Corrida: outro request criou o mesmo código entre a checagem e o insert
            throw new AliasAlreadyExistsException(code);
        }
    }

    @Transactional
    public String resolve(String code) {
        ShortUrl entity = repository.findByShortCode(code).orElseThrow(() -> {
            log.warn("Short URL not found: {}", code);
            return new ShortUrlNotFoundException(code);
        });
        if (entity.isExpired(Instant.now())) {
            log.info("Short URL expired: {}", code);
            throw new ShortUrlExpiredException(code);
        }
        repository.incrementClicks(entity.getId());
        redirectCounter.increment();
        log.debug("Redirecting {} -> {}", code, entity.getOriginalUrl());
        return entity.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlResponse getStats(String code) {
        ShortUrl entity = repository.findByShortCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
        return UrlResponse.from(entity, baseUrl);
    }

    @Transactional
    public void delete(String code) {
        ShortUrl entity = repository.findByShortCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
        repository.delete(entity);
        log.info("Short URL deleted: {}", code);
    }

    private String generateUniqueCode() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            if (!repository.existsByShortCode(code)) {
                return code;
            }
            log.debug("Code collision on attempt {}: {}", attempt, code);
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    private void validateUri(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) {
                throw new InvalidUrlException("url must contain a valid host");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("url is not a valid URI");
        }
    }
}
