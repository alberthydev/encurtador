package com.example.urlshortener.web;

import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortUrlExpiredException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ProblemDetail notFound(ShortUrlNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ShortUrlExpiredException.class)
    public ProblemDetail expired(ShortUrlExpiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ProblemDetail conflict(AliasAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ProblemDetail invalidUrl(InvalidUrlException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
