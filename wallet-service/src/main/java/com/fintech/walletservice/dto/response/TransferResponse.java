package com.fintech.walletservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transfer result")
public record TransferResponse(
        @Schema(description = "Unique transaction ID for this transfer", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID transactionId,

        @Schema(description = "Source wallet ID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID fromWalletId,

        @Schema(description = "Destination wallet ID", example = "770e8400-e29b-41d4-a716-446655440002")
        UUID toWalletId,

        @Schema(description = "Transferred amount in minor units", example = "5000")
        Long amount,

        @Schema(description = "Transfer status", example = "COMPLETED", allowableValues = {"COMPLETED", "PENDING", "FAILED"})
        String status
) {}
