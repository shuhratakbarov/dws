package com.fintech.walletservice.service;

import com.fintech.walletservice.config.TestcontainersConfiguration;
import com.fintech.walletservice.domain.LedgerEntry;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.exception.CurrencyMismatchException;
import com.fintech.walletservice.exception.InsufficientFundsException;
import com.fintech.walletservice.repository.LedgerEntryRepository;
import com.fintech.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class WalletTransferTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerRepository;

    @Test
    void shouldTransferFundsBetweenWallets() {
        // Setup
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = walletService.createWallet(userId, Wallet.Currency.USD);
        Wallet wallet2 = walletService.createWallet(UUID.randomUUID(), Wallet.Currency.USD);

        // Fund wallet1
        walletService.deposit(wallet1.getId(), 100000L, "deposit-1", "Initial");

        // Transfer
        UUID transactionId = walletService.transfer(
                wallet1.getId(),
                wallet2.getId(),
                30000L,
                "transfer-1",
                "Test transfer"
        );

        // Verify transaction ID exists
        assertThat(transactionId).isNotNull();

        // Verify balances
        Wallet updatedWallet1 = walletRepository.findById(wallet1.getId()).orElseThrow();
        Wallet updatedWallet2 = walletRepository.findById(wallet2.getId()).orElseThrow();

        assertThat(updatedWallet1.getBalanceMinorUnits()).isEqualTo(70000L); // 100000 - 30000
        assertThat(updatedWallet2.getBalanceMinorUnits()).isEqualTo(30000L);

        // Verify ledger entries
        List<LedgerEntry> entries = ledgerRepository.findByTransactionId(transactionId);
        assertThat(entries).hasSize(2);

        // Check debit entry
        LedgerEntry debitEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT)
                .findFirst()
                .orElseThrow();

        assertThat(debitEntry.getWalletId()).isEqualTo(wallet1.getId());
        assertThat(debitEntry.getAmountMinorUnits()).isEqualTo(30000L);
        assertThat(debitEntry.getBalanceAfter()).isEqualTo(70000L);

        // Check credit entry
        LedgerEntry creditEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT)
                .findFirst()
                .orElseThrow();

        assertThat(creditEntry.getWalletId()).isEqualTo(wallet2.getId());
        assertThat(creditEntry.getAmountMinorUnits()).isEqualTo(30000L);
        assertThat(creditEntry.getBalanceAfter()).isEqualTo(30000L);
    }

    @Test
    void shouldRejectTransferWhenInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = walletService.createWallet(userId, Wallet.Currency.USD);
        Wallet wallet2 = walletService.createWallet(UUID.randomUUID(), Wallet.Currency.USD);

        walletService.deposit(wallet1.getId(), 5000L, "deposit-1", "Initial");

        assertThatThrownBy(() ->
                walletService.transfer(wallet1.getId(), wallet2.getId(), 10000L, "transfer-1", "Too much")
        ).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldRejectTransferBetweenDifferentCurrencies() {
        UUID userId = UUID.randomUUID();
        Wallet usdWallet = walletService.createWallet(userId, Wallet.Currency.USD);
        Wallet eurWallet = walletService.createWallet(userId, Wallet.Currency.EUR);

        walletService.deposit(usdWallet.getId(), 10000L, "deposit-1", "Initial");

        assertThatThrownBy(() ->
                walletService.transfer(usdWallet.getId(), eurWallet.getId(), 5000L, "transfer-1", "Cross-currency")
        ).isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("Cannot transfer between different currencies");
    }

    @Test
    void shouldRejectTransferToSameWallet() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, Wallet.Currency.USD);

        assertThatThrownBy(() ->
                walletService.transfer(wallet.getId(), wallet.getId(), 5000L, "transfer-1", "Self-transfer")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to the same wallet");
    }

    @Test
    void shouldEnforceIdempotencyOnTransfer() {
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = walletService.createWallet(userId, Wallet.Currency.USD);
        Wallet wallet2 = walletService.createWallet(UUID.randomUUID(), Wallet.Currency.USD);

        walletService.deposit(wallet1.getId(), 100000L, "deposit-1", "Initial");

        String idempotencyKey = "transfer-duplicate-test";

        // First transfer
        UUID transactionId1 = walletService.transfer(
                wallet1.getId(), wallet2.getId(), 10000L, idempotencyKey, "Test"
        );

        // Second transfer with same key
        UUID transactionId2 = walletService.transfer(
                wallet1.getId(), wallet2.getId(), 10000L, idempotencyKey, "Test"
        );

        // Should return same transaction ID
        assertThat(transactionId2).isEqualTo(transactionId1);

        // Balance should only decrease once
        Wallet updatedWallet1 = walletRepository.findById(wallet1.getId()).orElseThrow();
        assertThat(updatedWallet1.getBalanceMinorUnits()).isEqualTo(90000L); // 100000 - 10000 (only once)

        Wallet updatedWallet2 = walletRepository.findById(wallet2.getId()).orElseThrow();
        assertThat(updatedWallet2.getBalanceMinorUnits()).isEqualTo(10000L);
    }

    @Test
    void shouldReconcileBothWalletsAfterTransfer() {
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = walletService.createWallet(userId, Wallet.Currency.USD);
        Wallet wallet2 = walletService.createWallet(UUID.randomUUID(), Wallet.Currency.USD);

        walletService.deposit(wallet1.getId(), 100000L, "deposit-1", "Initial");
        walletService.transfer(wallet1.getId(), wallet2.getId(), 30000L, "transfer-1", "Test");

        boolean reconciled1 = walletService.reconcileBalance(wallet1.getId());
        boolean reconciled2 = walletService.reconcileBalance(wallet2.getId());

        assertThat(reconciled1).isTrue();
        assertThat(reconciled2).isTrue();
    }
}