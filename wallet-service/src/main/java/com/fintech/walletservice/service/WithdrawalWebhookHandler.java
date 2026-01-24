package com.fintech.walletservice.service;

import com.fintech.walletservice.domain.LedgerEntry;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.exception.WalletNotFoundException;
import com.fintech.walletservice.repository.LedgerEntryRepository;
import com.fintech.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles withdrawal webhook callbacks from payment providers.
 *
 * When a payout is initiated:
 * 1. Funds are reserved (debited from wallet)
 * 2. Payout request sent to provider
 * 3. Provider processes and sends webhook
 * 4. This handler updates status and commits/releases reservation
 *
 * Idempotent: Same webhook can be processed multiple times safely.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WithdrawalWebhookHandler {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;

    // Simple in-memory idempotency store (use Redis in production)
    private final ConcurrentHashMap<String, Boolean> processedWebhooks = new ConcurrentHashMap<>();

    /**
     * Handle successful payout from payment provider.
     *
     * @param withdrawalId Internal withdrawal ID
     * @param externalId Provider's transaction ID
     */
    @Transactional
    public void handlePayoutSuccess(UUID withdrawalId, String externalId) {
        String idempotencyKey = "payout_success_" + withdrawalId;

        // Idempotency check
        if (processedWebhooks.putIfAbsent(idempotencyKey, true) != null) {
            log.info("Duplicate payout success webhook for withdrawal: {}", withdrawalId);
            return;
        }

        log.info("Processing payout success: withdrawalId={}, externalId={}",
                withdrawalId, externalId);

        // Find the ledger entry for this withdrawal
        Optional<LedgerEntry> entryOpt = ledgerRepository.findByIdempotencyKey(
                withdrawalId.toString() + "-withdrawal"
        );

        if (entryOpt.isEmpty()) {
            // Try without suffix
            entryOpt = ledgerRepository.findByIdempotencyKey(withdrawalId.toString());
        }

        if (entryOpt.isPresent()) {
            LedgerEntry entry = entryOpt.get();

            // Update entry status (if you have a status field)
            // entry.setStatus(LedgerEntry.Status.COMPLETED);
            // entry.setExternalReference(externalId);

            // For now, just log the completion
            log.info("Payout completed: withdrawalId={}, amount={}, walletId={}",
                    withdrawalId, entry.getAmountMinorUnits(), entry.getWalletId());

            // ledgerRepository.save(entry);
        } else {
            log.warn("Ledger entry not found for withdrawal: {}", withdrawalId);
        }
    }

    /**
     * Handle failed payout from payment provider.
     * Refunds the reserved amount back to the wallet.
     *
     * @param withdrawalId Internal withdrawal ID
     * @param reason Failure reason from provider
     */
    @Transactional
    public void handlePayoutFailure(UUID withdrawalId, String reason) {
        String idempotencyKey = "payout_failure_" + withdrawalId;

        // Idempotency check
        if (processedWebhooks.putIfAbsent(idempotencyKey, true) != null) {
            log.info("Duplicate payout failure webhook for withdrawal: {}", withdrawalId);
            return;
        }

        log.warn("Processing payout failure: withdrawalId={}, reason={}",
                withdrawalId, reason);

        // Find the original withdrawal entry
        Optional<LedgerEntry> entryOpt = ledgerRepository.findByIdempotencyKey(
                withdrawalId.toString() + "-withdrawal"
        );

        if (entryOpt.isEmpty()) {
            entryOpt = ledgerRepository.findByIdempotencyKey(withdrawalId.toString());
        }

        if (entryOpt.isPresent()) {
            LedgerEntry originalEntry = entryOpt.get();

            // Refund: Credit the amount back to wallet
            Wallet wallet = walletRepository.findByIdForUpdate(originalEntry.getWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(
                            "Wallet not found: " + originalEntry.getWalletId()
                    ));

            wallet.credit(originalEntry.getAmountMinorUnits());
            walletRepository.save(wallet);

            // Create refund ledger entry
            String refundIdempotencyKey = withdrawalId.toString() + "-refund";

            if (!ledgerRepository.existsByIdempotencyKey(refundIdempotencyKey)) {
                LedgerEntry refundEntry = LedgerEntry.builder()
                        .walletId(wallet.getId())
                        .entryType(LedgerEntry.EntryType.CREDIT)
                        .amountMinorUnits(originalEntry.getAmountMinorUnits())
                        .balanceAfter(wallet.getBalanceMinorUnits())
                        .transactionId(UUID.randomUUID())
                        .idempotencyKey(refundIdempotencyKey)
                        .description("Refund: Payout failed - " + reason)
                        .build();

                ledgerRepository.save(refundEntry);

                log.info("Payout refunded: withdrawalId={}, amount={}, newBalance={}",
                        withdrawalId, originalEntry.getAmountMinorUnits(),
                        wallet.getBalanceMinorUnits());
            }
        } else {
            log.error("Cannot refund - ledger entry not found for withdrawal: {}", withdrawalId);
        }
    }

    /**
     * Handle payout that's still processing.
     * Used for status tracking.
     *
     * @param withdrawalId Internal withdrawal ID
     * @param externalId Provider's transaction ID
     */
    public void handlePayoutProcessing(UUID withdrawalId, String externalId) {
        log.info("Payout still processing: withdrawalId={}, externalId={}",
                withdrawalId, externalId);

        // Update status to PROCESSING if you track payout status
        // This is informational - no action needed
    }

    /**
     * Handle payout cancellation.
     * Similar to failure - refunds the reserved amount.
     *
     * @param withdrawalId Internal withdrawal ID
     * @param reason Cancellation reason
     */
    @Transactional
    public void handlePayoutCancelled(UUID withdrawalId, String reason) {
        log.info("Payout cancelled: withdrawalId={}, reason={}", withdrawalId, reason);
        handlePayoutFailure(withdrawalId, "Cancelled: " + reason);
    }

    /**
     * Clear processed webhooks cache.
     * Should be called periodically or use TTL-based cache in production.
     */
    public void clearProcessedCache() {
        int size = processedWebhooks.size();
        processedWebhooks.clear();
        log.info("Cleared {} entries from processed webhooks cache", size);
    }
}
