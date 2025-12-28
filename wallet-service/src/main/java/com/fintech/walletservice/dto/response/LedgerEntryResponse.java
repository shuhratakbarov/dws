package com.fintech.walletservice.dto.response;

import com.fintech.walletservice.domain.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Ledger entry / transaction history item")
public record LedgerEntryResponse(
        @Schema(description = "Entry ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Transaction ID (same for both sides of transfer)", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID transactionId,

        @Schema(description = "Entry type", example = "CREDIT", allowableValues = {"DEBIT", "CREDIT"})
        String entryType,

        @Schema(description = "Amount in minor units", example = "10000")
        Long amount,

        @Schema(description = "Balance after this transaction", example = "150000")
        Long balanceAfter,

        @Schema(description = "Transaction description", example = "Monthly salary deposit")
        String description,

        @Schema(description = "When the transaction occurred", example = "2024-01-15T10:30:00Z")
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionId(),
                entry.getEntryType().name(),
                entry.getAmountMinorUnits(),
                entry.getBalanceAfter(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}

