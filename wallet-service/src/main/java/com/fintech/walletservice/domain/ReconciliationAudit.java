package com.fintech.walletservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for reconciliation mismatches.
 * Stores historical record of all detected discrepancies.
 */
@Entity
@Table(
        name = "reconciliation_audit",
        indexes = {
                @Index(name = "idx_audit_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private Long walletBalance;

    @Column(nullable = false)
    private Long ledgerBalance;

    @Column(nullable = false)
    private Long difference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReconciliationStatus status;

    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum ReconciliationStatus {
        DETECTED,      // Mismatch found
        INVESTIGATING, // Under review
        RESOLVED,      // Fixed
        FALSE_POSITIVE // Not actually a problem
    }
}
