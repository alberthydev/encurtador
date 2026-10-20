package com.example.urlshortener.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Adiciona um request_id ao MDC (vira campo pesquisável no Graylog) e loga cada requisição em DEBUG. */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("request_id", requestId);
        response.setHeader("X-Request-Id", requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            MDC.put("http_method", request.getMethod());
            MDC.put("http_status", String.valueOf(response.getStatus()));
            MDC.put("duration_ms", String.valueOf(ms));
            log.debug("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            MDC.clear();
        }
    }
}
