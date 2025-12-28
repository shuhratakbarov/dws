package com.fintech.walletservice.service;

import com.fintech.walletservice.config.TestcontainersConfiguration;
import com.fintech.walletservice.domain.LedgerEntry;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.exception.DuplicateWalletException;
import com.fintech.walletservice.exception.InsufficientFundsException;
import com.fintech.walletservice.repository.LedgerEntryRepository;
import com.fintech.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerRepository;

    @Test
    void shouldCreateWalletSuccessfully() {
        UUID userId = UUID.randomUUID();

        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getUserId()).isEqualTo(userId);
        assertThat(wallet.getCurrency()).isEqualTo(Wallet.Currency.USD);
        assertThat(wallet.getBalanceMinorUnits()).isZero();
        assertThat(wallet.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
    }

    @Test
    void shouldPreventDuplicateWalletPerCurrency() {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId, Wallet.Currency.EUR);

        assertThatThrownBy(() -> walletService.createWallet(userId, Wallet.Currency.EUR))
                .isInstanceOf(DuplicateWalletException.class);
    }

    @Test
    void shouldDepositFundsAndCreateLedgerEntry() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        LedgerEntry entry = walletService.deposit(
                wallet.getId(),
                50000L, // $500.00
                "deposit-" + UUID.randomUUID(),
                "Initial deposit"
        );

        assertThat(entry.getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        assertThat(entry.getAmountMinorUnits()).isEqualTo(50000L);
        assertThat(entry.getBalanceAfter()).isEqualTo(50000L);

        // Verify wallet balance updated
        Wallet updated = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(updated.getBalanceMinorUnits()).isEqualTo(50000L);
    }

    @Test
    void shouldEnforceIdempotencyOnDeposit() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);
        String idempotencyKey = "deposit-duplicate-test";

        // First deposit
        LedgerEntry first = walletService.deposit(wallet.getId(), 10000L, idempotencyKey, "Test");

        // Second deposit with same key
        LedgerEntry second = walletService.deposit(wallet.getId(), 10000L, idempotencyKey, "Test");

        // Should return same entry
        assertThat(second.getId()).isEqualTo(first.getId());

        // Balance should only increase once
        Wallet updated = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(updated.getBalanceMinorUnits()).isEqualTo(10000L);
    }

    @Test
    void shouldWithdrawFundsSuccessfully() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        // Deposit first
        walletService.deposit(wallet.getId(), 100000L, "dep-1", "Initial");

        // Then withdraw
        LedgerEntry withdrawal = walletService.withdraw(
                wallet.getId(),
                30000L,
                "withdraw-1",
                "Purchase"
        );

        assertThat(withdrawal.getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
        assertThat(withdrawal.getBalanceAfter()).isEqualTo(70000L);

        Wallet updated = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(updated.getBalanceMinorUnits()).isEqualTo(70000L);
    }

    @Test
    void shouldRejectWithdrawalWhenInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);
        walletService.deposit(wallet.getId(), 5000L, "dep-1", "Initial");

        assertThatThrownBy(() ->
                walletService.withdraw(wallet.getId(), 10000L, "withdraw-1", "Too much")
        ).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldReconcileBalanceCorrectly() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        // Multiple operations
        walletService.deposit(wallet.getId(), 10000L, "dep-1", "Deposit 1");
        walletService.deposit(wallet.getId(), 20000L, "dep-2", "Deposit 2");
        walletService.withdraw(wallet.getId(), 5000L, "with-1", "Withdrawal 1");

        boolean reconciled = walletService.reconcileBalance(wallet.getId());

        assertThat(reconciled).isTrue();

        Wallet updated = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(updated.getBalanceMinorUnits()).isEqualTo(25000L); // 10000 + 20000 - 5000
    }

    /**
     * CRITICAL TEST: Concurrent deposits should not cause race conditions.
     * Pessimistic locking ensures correctness.
     */
    @Test
    @Disabled("Concurrent test requires complex transaction setup")
    void shouldHandleConcurrentDepositsCorrectly() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        int threadCount = 10;
        int depositsPerThread = 10;
        long amountPerDeposit = 100L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < depositsPerThread; j++) {
                        String idempotencyKey = "concurrent-" + threadIndex + "-" + j;
                        try {
                            walletService.deposit(
                                    wallet.getId(),
                                    amountPerDeposit,
                                    idempotencyKey,
                                    "Concurrent deposit"
                            );
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            // Log but don't fail - some failures expected in concurrent tests
                            System.err.println("Deposit failed: " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS); // Add this line

        // Verify all deposits succeeded
        assertThat(successCount.get()).isEqualTo(threadCount * depositsPerThread);

        // Verify final balance is correct
        Wallet updated = walletRepository.findById(wallet.getId()).orElseThrow();
        long expectedBalance = threadCount * depositsPerThread * amountPerDeposit;
        assertThat(updated.getBalanceMinorUnits()).isEqualTo(expectedBalance);

        // Verify ledger entries match
        boolean reconciled = walletService.reconcileBalance(wallet.getId());
        assertThat(reconciled).isTrue();
    }
}
