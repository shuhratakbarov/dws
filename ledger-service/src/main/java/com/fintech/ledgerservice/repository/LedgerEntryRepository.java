package com.fintech.ledgerservice.repository;

import com.fintech.ledgerservice.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    // ==================== By Wallet ====================

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    List<LedgerEntry> findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId, Instant startDate, Instant endDate);

    // ==================== By User ====================

    Page<LedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // ==================== By Transaction ====================

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    // ==================== Idempotency ====================

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    // ==================== Balance Calculation ====================

    /**
     * Calculate balance from ledger entries.
     * CREDIT adds, DEBIT subtracts.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amountMinorUnits " +
           "ELSE -e.amountMinorUnits END), 0) FROM LedgerEntry e WHERE e.walletId = :walletId")
    Long calculateBalanceByWalletId(UUID walletId);

    // ==================== Reporting ====================

    @Query("SELECT e.transactionType, COUNT(e), SUM(e.amountMinorUnits) " +
           "FROM LedgerEntry e WHERE e.walletId = :walletId " +
           "AND e.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY e.transactionType")
    List<Object[]> getTransactionSummaryByWallet(UUID walletId, Instant startDate, Instant endDate);

    @Query("SELECT COUNT(e) FROM LedgerEntry e WHERE e.walletId = :walletId")
    long countByWalletId(UUID walletId);
}

