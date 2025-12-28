package com.fintech.ledgerservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code")
        String code,

        @Schema(description = "Error message")
        String message,

        @Schema(description = "Timestamp")
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}

