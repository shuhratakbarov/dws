package com.fintech.ledgerservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry representing a financial transaction.
 *
 * CRITICAL: Ledger entries are NEVER modified or deleted.
 * They form an append-only audit trail for all financial movements.
 *
 * Double-entry principle:
 * - Every transfer creates TWO entries (DEBIT from source, CREDIT to destination)
 * - Both entries share the same transactionId
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_wallet", columnList = "walletId"),
        @Index(name = "idx_ledger_transaction", columnList = "transactionId"),
        @Index(name = "idx_ledger_created", columnList = "createdAt"),
        @Index(name = "idx_ledger_idempotency", columnList = "idempotencyKey", unique = true)
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The wallet this entry affects.
     */
    @Column(nullable = false)
    private UUID walletId;

    /**
     * User who owns the wallet (denormalized for query efficiency).
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * Type of entry: DEBIT (money out) or CREDIT (money in).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    /**
     * Transaction type for categorization.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    /**
     * Amount in minor units (cents). Always positive.
     */
    @Column(nullable = false)
    private Long amountMinorUnits;

    /**
     * Currency of the transaction.
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Balance after this transaction.
     */
    @Column(nullable = false)
    private Long balanceAfter;

    /**
     * Links related entries (e.g., both sides of a transfer).
     */
    @Column(nullable = false)
    private UUID transactionId;

    /**
     * For transfers: the counterparty wallet.
     */
    private UUID counterpartyWalletId;

    /**
     * Ensures idempotency - prevents duplicate processing.
     */
    @Column(unique = true)
    private String idempotencyKey;

    /**
     * Human-readable description.
     */
    private String description;

    /**
     * Reference to external system (e.g., payment provider ID).
     */
    private String externalReference;

    /**
     * Immutable timestamp - set once, never modified.
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // ======= Enums =======

    public enum EntryType {
        DEBIT,   // Money going OUT
        CREDIT   // Money coming IN
    }

    public enum TransactionType {
        DEPOSIT,     // External money in
        WITHDRAWAL,  // External money out
        TRANSFER_IN, // Transfer from another wallet
        TRANSFER_OUT,// Transfer to another wallet
        FEE,         // Service fee
        REFUND,      // Refund of previous transaction
        ADJUSTMENT   // Manual adjustment (admin)
    }
}

