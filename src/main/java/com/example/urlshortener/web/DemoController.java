package com.example.urlshortener.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de demonstração para gerar logs em todos os níveis e erros 5xx,
 * úteis para evidenciar Graylog e Grafana.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/logs")
    public Map<String, String> logs() {
        log.debug("Demo DEBUG message");
        log.info("Demo INFO message");
        log.warn("Demo WARN message");
        log.error("Demo ERROR message");
        return Map.of("status", "logs emitted");
    }

    @GetMapping("/error")
    public void error() {
        log.info("Simulating an internal failure (HTTP 500)");
        throw new IllegalStateException("Simulated failure for demo purposes");
    }
}
