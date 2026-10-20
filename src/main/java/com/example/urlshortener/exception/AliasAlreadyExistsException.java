package com.example.urlshortener.exception;

public class AliasAlreadyExistsException extends RuntimeException {
    public AliasAlreadyExistsException(String alias) {
        super("Alias already in use: " + alias);
    }
}
