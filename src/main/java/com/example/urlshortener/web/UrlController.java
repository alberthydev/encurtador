package com.example.urlshortener.web;

import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlShortenerService service;

    public UrlController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = service.create(request);
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    @GetMapping("/api/v1/urls/{code}")
    public UrlResponse stats(@PathVariable String code) {
        return service.getStats(code);
    }

    @DeleteMapping("/api/v1/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = service.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
