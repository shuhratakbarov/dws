package com.fintech.ledgerservice.dto.response;

import com.fintech.ledgerservice.domain.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Ledger entry response")
public record LedgerEntryResponse(
        @Schema(description = "Entry ID")
        UUID id,

        @Schema(description = "Wallet ID")
        UUID walletId,

        @Schema(description = "User ID")
        UUID userId,

        @Schema(description = "Entry type: DEBIT or CREDIT")
        String entryType,

        @Schema(description = "Transaction type")
        String transactionType,

        @Schema(description = "Amount in minor units")
        Long amountMinorUnits,

        @Schema(description = "Currency")
        String currency,

        @Schema(description = "Balance after transaction")
        Long balanceAfter,

        @Schema(description = "Transaction ID")
        UUID transactionId,

        @Schema(description = "Counterparty wallet (for transfers)")
        UUID counterpartyWalletId,

        @Schema(description = "Description")
        String description,

        @Schema(description = "External reference")
        String externalReference,

        @Schema(description = "When the entry was created")
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getWalletId(),
                entry.getUserId(),
                entry.getEntryType().name(),
                entry.getTransactionType().name(),
                entry.getAmountMinorUnits(),
                entry.getCurrency(),
                entry.getBalanceAfter(),
                entry.getTransactionId(),
                entry.getCounterpartyWalletId(),
                entry.getDescription(),
                entry.getExternalReference(),
                entry.getCreatedAt()
        );
    }
}

