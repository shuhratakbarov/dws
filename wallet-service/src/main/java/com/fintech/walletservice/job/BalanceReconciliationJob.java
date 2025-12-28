package com.fintech.walletservice.job;

import com.fintech.walletservice.domain.ReconciliationAudit;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.repository.LedgerEntryRepository;
import com.fintech.walletservice.repository.ReconciliationAuditRepository;
import com.fintech.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Scheduled job to reconcile wallet balances against ledger entries.
 * Runs daily at 2 AM to detect any data inconsistencies.
 *
 * In production, this should:
 * 1. Send alerts to ops team when mismatches found
 * 2. Write to separate audit log
 * 3. Potentially auto-correct or flag wallets for manual review
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceReconciliationJob {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final ReconciliationAuditRepository auditRepository;

    /**
     * Run reconciliation daily at 2:00 AM.
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void reconcileAllWallets() {
        log.info("Starting daily balance reconciliation job...");

        long startTime = System.currentTimeMillis();
        List<Wallet> allWallets = walletRepository.findAll();

        int totalWallets = allWallets.size();
        int mismatchCount = 0;

        for (Wallet wallet : allWallets) {
            try {
                boolean reconciled = reconcileWallet(wallet);
                if (!reconciled) {
                    mismatchCount++;
                }
            } catch (Exception e) {
                log.error("Error reconciling wallet {}: {}", wallet.getId(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        if (mismatchCount > 0) {
            log.error("❌ BALANCE RECONCILIATION FAILED! {} out of {} wallets have mismatches. Duration: {}ms",
                    mismatchCount, totalWallets, duration);
            // TODO: Send alert to ops team (email, Slack, PagerDuty)
        } else {
            log.info("✅ Balance reconciliation completed successfully. {} wallets checked. Duration: {}ms",
                    totalWallets, duration);
        }
    }

    /**
     * Alternative: Run every 6 hours (for testing/high-frequency monitoring)
     * Uncomment this and comment out the daily cron above
     */
    // @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    // @Transactional(readOnly = true)
    // public void reconcileAllWalletsFrequent() {
    //     reconcileAllWallets();
    // }

    /**
     * Reconcile a single wallet.
     * @return true if balance matches, false if mismatch detected
     */
    private boolean reconcileWallet(Wallet wallet) {
        UUID walletId = wallet.getId();
        Long walletBalance = wallet.getBalanceMinorUnits();
        Long ledgerBalance = ledgerRepository.calculateBalanceFromLedger(walletId);

        if (ledgerBalance == null) {
            ledgerBalance = 0L;
        }

        if (!walletBalance.equals(ledgerBalance)) {
            long difference = walletBalance - ledgerBalance;

            log.error("❌ BALANCE MISMATCH DETECTED! Wallet {}: wallet_balance={}, ledger_balance={}, difference={}",
                    walletId, walletBalance, ledgerBalance, Math.abs(difference));

            // Save audit record for investigation
            ReconciliationAudit audit = ReconciliationAudit.builder()
                    .walletId(walletId)
                    .walletBalance(walletBalance)
                    .ledgerBalance(ledgerBalance)
                    .difference(difference)
                    .status(ReconciliationAudit.ReconciliationStatus.DETECTED)
                    .notes("Automated detection during scheduled reconciliation")
                    .build();

            auditRepository.save(audit);

            // TODO: Send immediate alert to ops team

            return false;
        }

        return true;
    }

    /**
     * Manual trigger endpoint for on-demand reconciliation.
     * Can be called via REST API for testing or emergency checks.
     */
    public ReconciliationReport reconcileAllWalletsManually() {
        log.info("Manual reconciliation triggered");

        long startTime = System.currentTimeMillis();
        List<Wallet> allWallets = walletRepository.findAll();

        int totalWallets = allWallets.size();
        int successCount = 0;
        int failureCount = 0;

        for (Wallet wallet : allWallets) {
            try {
                boolean reconciled = reconcileWallet(wallet);
                if (reconciled) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Error reconciling wallet {}: {}", wallet.getId(), e.getMessage(), e);
                failureCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return new ReconciliationReport(
                totalWallets,
                successCount,
                failureCount,
                duration,
                failureCount == 0
        );
    }

    public record ReconciliationReport(
            int totalWallets,
            int successCount,
            int failureCount,
            long durationMs,
            boolean allReconciled
    ) {}
}
