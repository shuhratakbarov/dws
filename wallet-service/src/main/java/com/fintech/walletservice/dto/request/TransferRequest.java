package com.fintech.walletservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Request to transfer funds between wallets")
public record TransferRequest(
        @Schema(description = "Source wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID fromWalletId,

        @Schema(description = "Destination wallet ID", example = "660e8400-e29b-41d4-a716-446655440001")
        @NotNull UUID toWalletId,

        @Schema(description = "Amount in minor units", example = "5000", minimum = "1")
        @NotNull @Positive Long amountMinorUnits,

        @Schema(description = "Unique key to prevent duplicate transfers", example = "transfer-abc123-2024-01-15")
        @NotBlank String idempotencyKey,

        @Schema(description = "Optional description", example = "Payment for services")
        String description
) {}