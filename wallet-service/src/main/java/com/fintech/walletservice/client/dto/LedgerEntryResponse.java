package com.fintech.walletservice.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO from Ledger Service.
 */
public record LedgerEntryResponse(
        UUID id,
        UUID walletId,
        UUID userId,
        String entryType,
        String transactionType,
        Long amountMinorUnits,
        String currency,
        Long balanceAfter,
        UUID transactionId,
        UUID counterpartyWalletId,
        String description,
        String externalReference,
        Instant createdAt
) {}

