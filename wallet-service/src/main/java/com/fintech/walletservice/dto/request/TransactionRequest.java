package com.fintech.walletservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request for deposit or withdrawal")
public record TransactionRequest(
        @Schema(
                description = "Amount in minor units (cents/tiyin). Example: 1050 = $10.50",
                example = "10000",
                minimum = "1"
        )
        @NotNull @Positive Long amountMinorUnits,

        @Schema(
                description = "Unique key to prevent duplicate transactions. Use UUID or timestamp-based key.",
                example = "deposit-user123-2024-01-15-001"
        )
        @NotBlank String idempotencyKey,

        @Schema(
                description = "Optional description for the transaction",
                example = "Monthly salary deposit"
        )
        String description
) {}
