package com.fintech.authservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code", example = "AUTHENTICATION_FAILED")
        String code,

        @Schema(description = "Error message", example = "Invalid email or password")
        String message,

        @Schema(description = "Timestamp", example = "2024-01-15T10:30:00Z")
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}

