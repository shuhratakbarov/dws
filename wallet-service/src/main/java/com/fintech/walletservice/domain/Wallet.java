package com.fintech.walletservice.domain;

import com.fintech.walletservice.exception.InsufficientFundsException;
import com.fintech.walletservice.exception.WalletNotActiveException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency"}),
        indexes = {
                @Index(name = "idx_wallet_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    // CRITICAL: Store as cents to avoid floating point errors
    @Column(nullable = false)
    @Builder.Default
    private Long balanceMinorUnits = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Version // Optimistic locking
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public enum Currency {
        USD, EUR, UZS
    }

    public enum WalletStatus {
        ACTIVE, FROZEN, CLOSED
    }

    // Business logic: Check if wallet can perform operations
    public void validateActive() {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException("Wallet " + id + " is " + status);
        }
    }

    // Business logic: Debit funds (with validation)
    public void debit(Long amount) {
        validateActive();
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (balanceMinorUnits < amount) {
            throw new InsufficientFundsException(
                    "Insufficient balance. Required: " + amount + ", Available: " + balanceMinorUnits
            );
        }
        balanceMinorUnits -= amount;
    }

    // Business logic: Credit funds
    public void credit(Long amount) {
        validateActive();
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        balanceMinorUnits += amount;
    }
}
