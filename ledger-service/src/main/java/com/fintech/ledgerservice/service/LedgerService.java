package com.fintech.ledgerservice.service;

import com.fintech.ledgerservice.domain.LedgerEntry;
import com.fintech.ledgerservice.dto.request.CreateLedgerEntryRequest;
import com.fintech.ledgerservice.dto.response.BalanceResponse;
import com.fintech.ledgerservice.exception.DuplicateLedgerEntryException;
import com.fintech.ledgerservice.exception.LedgerEntryNotFoundException;
import com.fintech.ledgerservice.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Create a new ledger entry.
     * IDEMPOTENT: Same idempotencyKey returns existing entry.
     */
    @Transactional
    public LedgerEntry createEntry(CreateLedgerEntryRequest request) {
        // Check idempotency
        Optional<LedgerEntry> existing = ledgerEntryRepository
                .findByIdempotencyKey(request.idempotencyKey());

        if (existing.isPresent()) {
            log.info("Returning existing entry for idempotency key: {}", request.idempotencyKey());
            return existing.get();
        }

        LedgerEntry entry = LedgerEntry.builder()
                .walletId(request.walletId())
                .userId(request.userId())
                .entryType(request.entryType())
                .transactionType(request.transactionType())
                .amountMinorUnits(request.amountMinorUnits())
                .currency(request.currency())
                .balanceAfter(request.balanceAfter())
                .transactionId(request.transactionId())
                .counterpartyWalletId(request.counterpartyWalletId())
                .idempotencyKey(request.idempotencyKey())
                .description(request.description())
                .externalReference(request.externalReference())
                .build();

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        log.info("Created ledger entry {} for wallet {}", saved.getId(), saved.getWalletId());
        return saved;
    }

    /**
     * Get entry by ID.
     */
    @Transactional(readOnly = true)
    public LedgerEntry getEntry(UUID entryId) {
        return ledgerEntryRepository.findById(entryId)
                .orElseThrow(() -> new LedgerEntryNotFoundException(
                        "Ledger entry not found: " + entryId));
    }

    /**
     * Get transaction history for a wallet.
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntry> getWalletHistory(UUID walletId, Pageable pageable) {
        return ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    /**
     * Get transaction history for a user (all wallets).
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntry> getUserHistory(UUID userId, Pageable pageable) {
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get all entries for a transaction (e.g., both sides of a transfer).
     */
    @Transactional(readOnly = true)
    public List<LedgerEntry> getTransactionEntries(UUID transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }

    /**
     * Get wallet history for a date range.
     */
    @Transactional(readOnly = true)
    public List<LedgerEntry> getWalletHistoryByDateRange(
            UUID walletId, Instant startDate, Instant endDate) {
        return ledgerEntryRepository.findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                walletId, startDate, endDate);
    }

    /**
     * Calculate balance from ledger entries.
     * This is the "source of truth" - should match wallet balance.
     */
    @Transactional(readOnly = true)
    public BalanceResponse calculateBalance(UUID walletId) {
        Long balance = ledgerEntryRepository.calculateBalanceByWalletId(walletId);
        long txCount = ledgerEntryRepository.countByWalletId(walletId);

        return new BalanceResponse(
                walletId,
                balance,
                txCount,
                Instant.now()
        );
    }

    /**
     * Check if idempotency key exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return ledgerEntryRepository.existsByIdempotencyKey(idempotencyKey);
    }
}

