package com.fintech.ledgerservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Balance calculated from ledger")
public record BalanceResponse(
        @Schema(description = "Wallet ID")
        UUID walletId,

        @Schema(description = "Calculated balance in minor units")
        Long balanceMinorUnits,

        @Schema(description = "Total number of transactions")
        Long transactionCount,

        @Schema(description = "When balance was calculated")
        Instant calculatedAt
) {}

