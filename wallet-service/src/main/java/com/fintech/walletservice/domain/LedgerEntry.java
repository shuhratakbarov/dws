package com.fintech.walletservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ledger_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key"}),
        indexes = {
                @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        }
)
@Immutable // Hibernate optimization: never update
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Column(nullable = false)
    private Long amountMinorUnits;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum EntryType {
        DEBIT,  // Money out
        CREDIT  // Money in
    }
}
