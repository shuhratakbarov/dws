package com.fintech.walletservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code", example = "INSUFFICIENT_FUNDS")
        String code,

        @Schema(description = "Human-readable error message", example = "Insufficient funds: required 10000, available 5000")
        String message,

        @Schema(description = "Timestamp when error occurred", example = "2024-01-15T10:30:00Z")
        Instant timestamp
) {}
