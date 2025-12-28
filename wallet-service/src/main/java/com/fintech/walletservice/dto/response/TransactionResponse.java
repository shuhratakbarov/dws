package com.fintech.walletservice.dto.response;

import com.fintech.walletservice.domain.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transaction result")
public record TransactionResponse(
        @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"DEBIT", "CREDIT"})
        String type,

        @Schema(description = "Amount in minor units", example = "10000")
        Long amount,

        @Schema(description = "Balance after transaction in minor units", example = "150000")
        Long balanceAfter
) {
    public static TransactionResponse from(LedgerEntry e) {
        return new TransactionResponse(e.getId(), e.getEntryType().name(),
                e.getAmountMinorUnits(), e.getBalanceAfter());
    }
}