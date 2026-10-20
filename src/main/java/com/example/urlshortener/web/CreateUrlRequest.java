package com.example.urlshortener.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must have at most 2048 characters")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$",
                message = "customAlias must have 3-32 chars: letters, digits, '_' or '-'")
        String customAlias,

        @Min(value = 1, message = "expiresInDays must be >= 1")
        @Max(value = 3650, message = "expiresInDays must be <= 3650")
        Integer expiresInDays
) {
}
