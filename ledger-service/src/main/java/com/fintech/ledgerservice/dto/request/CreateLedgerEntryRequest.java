package com.fintech.ledgerservice.dto.request;

import com.fintech.ledgerservice.domain.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create a ledger entry")
public record CreateLedgerEntryRequest(
        @Schema(description = "Wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Wallet ID is required")
        UUID walletId,

        @Schema(description = "User ID who owns the wallet")
        @NotNull(message = "User ID is required")
        UUID userId,

        @Schema(description = "Entry type", example = "CREDIT")
        @NotNull(message = "Entry type is required")
        LedgerEntry.EntryType entryType,

        @Schema(description = "Transaction type", example = "DEPOSIT")
        @NotNull(message = "Transaction type is required")
        LedgerEntry.TransactionType transactionType,

        @Schema(description = "Amount in minor units", example = "10000")
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be positive")
        Long amountMinorUnits,

        @Schema(description = "Currency code", example = "USD")
        @NotBlank(message = "Currency is required")
        String currency,

        @Schema(description = "Balance after transaction", example = "50000")
        @NotNull(message = "Balance after is required")
        Long balanceAfter,

        @Schema(description = "Transaction ID (links related entries)")
        @NotNull(message = "Transaction ID is required")
        UUID transactionId,

        @Schema(description = "Counterparty wallet (for transfers)")
        UUID counterpartyWalletId,

        @Schema(description = "Idempotency key for duplicate prevention")
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @Schema(description = "Description", example = "Monthly salary deposit")
        String description,

        @Schema(description = "External reference (e.g., payment provider ID)")
        String externalReference
) {}

