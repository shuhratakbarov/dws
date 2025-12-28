package com.fintech.walletservice.client.dto;

import java.util.UUID;

/**
 * Request DTO for creating a ledger entry.
 * Mirrors the Ledger Service's CreateLedgerEntryRequest.
 */
public record CreateLedgerEntryRequest(
        UUID walletId,
        UUID userId,
        String entryType,       // CREDIT or DEBIT
        String transactionType, // DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
        Long amountMinorUnits,
        String currency,
        Long balanceAfter,
        UUID transactionId,
        UUID counterpartyWalletId,
        String idempotencyKey,
        String description,
        String externalReference
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID walletId;
        private UUID userId;
        private String entryType;
        private String transactionType;
        private Long amountMinorUnits;
        private String currency;
        private Long balanceAfter;
        private UUID transactionId;
        private UUID counterpartyWalletId;
        private String idempotencyKey;
        private String description;
        private String externalReference;

        public Builder walletId(UUID walletId) { this.walletId = walletId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder entryType(String entryType) { this.entryType = entryType; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder amountMinorUnits(Long amountMinorUnits) { this.amountMinorUnits = amountMinorUnits; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder balanceAfter(Long balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public Builder transactionId(UUID transactionId) { this.transactionId = transactionId; return this; }
        public Builder counterpartyWalletId(UUID counterpartyWalletId) { this.counterpartyWalletId = counterpartyWalletId; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder externalReference(String externalReference) { this.externalReference = externalReference; return this; }

        public CreateLedgerEntryRequest build() {
            return new CreateLedgerEntryRequest(
                    walletId, userId, entryType, transactionType, amountMinorUnits,
                    currency, balanceAfter, transactionId, counterpartyWalletId,
                    idempotencyKey, description, externalReference
            );
        }
    }
}

