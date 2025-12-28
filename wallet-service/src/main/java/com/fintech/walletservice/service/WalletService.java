package com.fintech.walletservice.service;

import com.fintech.walletservice.client.LedgerClient;
import com.fintech.walletservice.client.NotificationClient;
import com.fintech.walletservice.client.dto.CreateLedgerEntryRequest;
import com.fintech.walletservice.domain.LedgerEntry;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.exception.CurrencyMismatchException;
import com.fintech.walletservice.exception.DuplicateWalletException;
import com.fintech.walletservice.exception.UnauthorizedAccessException;
import com.fintech.walletservice.exception.WalletNotFoundException;
import com.fintech.walletservice.repository.LedgerEntryRepository;
import com.fintech.walletservice.repository.WalletRepository;
import com.fintech.walletservice.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final LedgerClient ledgerClient;
    private final NotificationClient notificationClient;

    // Threshold for large transaction alerts (in minor units)
    private final long largeTransactionThreshold;

    public WalletService(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerRepository,
            LedgerClient ledgerClient,
            NotificationClient notificationClient,
            @Value("${wallet.large-transaction-threshold:100000}") long largeTransactionThreshold
    ) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.ledgerClient = ledgerClient;
        this.notificationClient = notificationClient;
        this.largeTransactionThreshold = largeTransactionThreshold;
    }

    /**
     * Create a new wallet for a user in specified currency.
     * ONE wallet per user per currency.
     */
    @Transactional
    public Wallet createWallet(UUID userId, Wallet.Currency currency) {
        if (walletRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new DuplicateWalletException(
                    "User already has a " + currency + " wallet"
            );
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency(currency)
                .balanceMinorUnits(0L)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Created wallet {} for user {} in {}", saved.getId(), userId, currency);
        return saved;
    }

    /**
     * Deposit funds into wallet.
     * IDEMPOTENT: Same idempotency key = same result.
     */
    @Transactional
    public LedgerEntry deposit(UUID walletId, Long amount, String idempotencyKey, String description) {
        // Check idempotency
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate deposit request with key: {}", idempotencyKey);
            return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key exists but entry not found"));
        }

        // Pessimistic lock prevents race conditions
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.credit(amount);
        walletRepository.save(wallet);

        // Immutable audit trail
        LedgerEntry entry = LedgerEntry.builder()
                .walletId(walletId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amountMinorUnits(amount)
                .balanceAfter(wallet.getBalanceMinorUnits())
                .transactionId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .description(description)
                .build();

        LedgerEntry saved = ledgerRepository.save(entry);
        log.info("Deposited {} to wallet {}. New balance: {}",
                amount, walletId, wallet.getBalanceMinorUnits());

        // Record in centralized Ledger Service (async, non-blocking)
        recordToLedgerService(wallet, saved, null);

        // Send notification
        sendDepositNotification(wallet, saved);

        return saved;
    }

    /**
     * Withdraw funds from wallet.
     * IDEMPOTENT with idempotency key.
     */
    @Transactional
    public LedgerEntry withdraw(UUID walletId, Long amount, String idempotencyKey, String description) {
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate withdrawal request with key: {}", idempotencyKey);
            return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key exists but entry not found"));
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.debit(amount);
        walletRepository.save(wallet);

        LedgerEntry entry = LedgerEntry.builder()
                .walletId(walletId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amountMinorUnits(amount)
                .balanceAfter(wallet.getBalanceMinorUnits())
                .transactionId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .description(description)
                .build();

        LedgerEntry saved = ledgerRepository.save(entry);
        log.info("Withdrew {} from wallet {}. New balance: {}",
                amount, walletId, wallet.getBalanceMinorUnits());

        // Record in centralized Ledger Service
        recordToLedgerService(wallet, saved, null);

        // Send notification
        sendWithdrawalNotification(wallet, saved);

        return saved;
    }

    /**
     * CRITICAL: Balance reconciliation.
     * Wallet balance must match ledger entries.
     */
    public boolean reconcileBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        Long ledgerBalance = ledgerRepository.calculateBalanceFromLedger(walletId);
        Long walletBalance = wallet.getBalanceMinorUnits();

        if (!walletBalance.equals(ledgerBalance)) {
            log.error("BALANCE MISMATCH! Wallet {}: wallet={}, ledger={}",
                    walletId, walletBalance, ledgerBalance);
            return false;
        }

        return true;
    }

    /**
     * Transfer funds between two wallets.
     * ATOMIC: Both debit and credit happen in same transaction.
     * IDEMPOTENT: Same idempotency key = same result.
     *
     * @param fromWalletId Source wallet
     * @param toWalletId Destination wallet
     * @param amount Amount in minor units
     * @param idempotencyKey Unique key to prevent duplicate transfers
     * @param description Transfer description
     * @return Transaction ID that groups both ledger entries
     */
    @Transactional
    public UUID transfer(
            UUID fromWalletId,
            UUID toWalletId,
            Long amount,
            String idempotencyKey,
            String description
    ) {
        // Validate input
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Check idempotency - use a combined key for the transfer
        String debitIdempotencyKey = idempotencyKey + "-debit";
        String creditIdempotencyKey = idempotencyKey + "-credit";

        if (ledgerRepository.existsByIdempotencyKey(debitIdempotencyKey)) {
            log.warn("Duplicate transfer request with key: {}", idempotencyKey);
            // Return existing transaction ID
            LedgerEntry existingEntry = ledgerRepository.findByIdempotencyKey(debitIdempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key exists but entry not found"));
            return existingEntry.getTransactionId();
        }

        // Lock both wallets in consistent order to prevent deadlocks
        // Always lock smaller UUID first
        UUID firstLock = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondLock = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstLock)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + firstLock));

        Wallet secondWallet = walletRepository.findByIdForUpdate(secondLock)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + secondLock));

        // Get actual from/to wallets after locking
        Wallet fromWallet = fromWalletId.equals(firstLock) ? firstWallet : secondWallet;
        Wallet toWallet = toWalletId.equals(firstLock) ? firstWallet : secondWallet;

        // Validate currencies match
        if (fromWallet.getCurrency() != toWallet.getCurrency()) {
            throw new CurrencyMismatchException(
                    "Cannot transfer between different currencies: " +
                    fromWallet.getCurrency() + " -> " + toWallet.getCurrency()
            );
        }

        // Generate single transaction ID for both entries
        UUID transactionId = UUID.randomUUID();

        // Debit from source wallet
        fromWallet.debit(amount);
        walletRepository.save(fromWallet);

        LedgerEntry debitEntry = LedgerEntry.builder()
                .walletId(fromWalletId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amountMinorUnits(amount)
                .balanceAfter(fromWallet.getBalanceMinorUnits())
                .transactionId(transactionId)
                .idempotencyKey(debitIdempotencyKey)
                .description("Transfer to wallet " + toWalletId + ": " + description)
                .build();

        ledgerRepository.save(debitEntry);

        // Credit to destination wallet
        toWallet.credit(amount);
        walletRepository.save(toWallet);

        LedgerEntry creditEntry = LedgerEntry.builder()
                .walletId(toWalletId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amountMinorUnits(amount)
                .balanceAfter(toWallet.getBalanceMinorUnits())
                .transactionId(transactionId)
                .idempotencyKey(creditIdempotencyKey)
                .description("Transfer from wallet " + fromWalletId + ": " + description)
                .build();

        ledgerRepository.save(creditEntry);

        log.info("Transfer completed: {} from wallet {} to wallet {}. Transaction ID: {}",
                amount, fromWalletId, toWalletId, transactionId);

        // Record both sides of transfer in Ledger Service
        recordTransferToLedgerService(fromWallet, debitEntry, toWalletId);
        recordTransferToLedgerService(toWallet, creditEntry, fromWalletId);

        // Send notifications to both parties
        sendTransferNotifications(fromWallet, toWallet, debitEntry, creditEntry, amount, transactionId);

        return transactionId;
    }

    /**
     * Get wallet by ID with ownership check.
     * Users can only view their own wallets (unless admin).
     */
    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        verifyWalletAccess(wallet);
        return wallet;
    }

    /**
     * Get wallet by ID without ownership check (for internal use).
     */
    @Transactional(readOnly = true)
    public Wallet getWalletInternal(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }

    /**
     * Get all wallets for a user.
     * Users can only view their own wallets.
     */
    @Transactional(readOnly = true)
    public java.util.List<Wallet> getWalletsByUser(UUID userId) {
        verifyUserAccess(userId);
        return walletRepository.findByUserId(userId);
    }

    /**
     * Get transaction history for a wallet with pagination.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<LedgerEntry> getTransactionHistory(
            UUID walletId,
            org.springframework.data.domain.Pageable pageable) {
        // Verify wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException("Wallet not found: " + walletId);
        }
        return ledgerRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    /**
     * Freeze a wallet (admin operation).
     * Frozen wallets cannot perform transactions.
     */
    @Transactional
    public Wallet freezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        Wallet saved = walletRepository.save(wallet);
        log.warn("Wallet {} has been FROZEN", walletId);
        return saved;
    }

    /**
     * Unfreeze a wallet (admin operation).
     */
    @Transactional
    public Wallet unfreezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet {} has been UNFROZEN", walletId);
        return saved;
    }

    // ==================== Security Helper Methods ====================

    /**
     * Verify that the current user has access to the wallet.
     * Admins can access any wallet.
     */
    private void verifyWalletAccess(Wallet wallet) {
        if (!UserContext.isAuthenticated()) {
            // If no user context (direct access without gateway), allow for backward compatibility
            log.debug("No user context - allowing access (direct access mode)");
            return;
        }

        if (UserContext.isAdmin()) {
            log.debug("Admin access to wallet {}", wallet.getId());
            return;
        }

        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(wallet.getUserId())) {
            log.warn("User {} attempted to access wallet {} owned by {}",
                    currentUserId, wallet.getId(), wallet.getUserId());
            throw new UnauthorizedAccessException("You don't have access to this wallet");
        }
    }

    /**
     * Verify that the current user can access data for the specified userId.
     */
    private void verifyUserAccess(UUID userId) {
        if (!UserContext.isAuthenticated()) {
            return; // Allow direct access
        }

        if (UserContext.isAdmin()) {
            return;
        }

        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            log.warn("User {} attempted to access data for user {}", currentUserId, userId);
            throw new UnauthorizedAccessException("You can only access your own data");
        }
    }

    // ==================== Ledger Service Integration ====================

    /**
     * Record a transaction to the centralized Ledger Service.
     * This is done asynchronously - failure does not block the main transaction.
     */
    private void recordToLedgerService(Wallet wallet, LedgerEntry entry, UUID counterpartyWalletId) {
        try {
            String transactionType = entry.getEntryType() == LedgerEntry.EntryType.CREDIT
                    ? "DEPOSIT" : "WITHDRAWAL";

            CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                    .walletId(wallet.getId())
                    .userId(wallet.getUserId())
                    .entryType(entry.getEntryType().name())
                    .transactionType(transactionType)
                    .amountMinorUnits(entry.getAmountMinorUnits())
                    .currency(wallet.getCurrency().name())
                    .balanceAfter(entry.getBalanceAfter())
                    .transactionId(entry.getTransactionId())
                    .counterpartyWalletId(counterpartyWalletId)
                    .idempotencyKey(entry.getIdempotencyKey())
                    .description(entry.getDescription())
                    .build();

            ledgerClient.recordTransaction(request);
        } catch (Exception e) {
            // Log but don't fail the transaction
            log.error("Failed to record to Ledger Service: {}", e.getMessage());
        }
    }

    /**
     * Record a transfer entry to the Ledger Service.
     */
    private void recordTransferToLedgerService(Wallet wallet, LedgerEntry entry, UUID counterpartyWalletId) {
        try {
            String transactionType = entry.getEntryType() == LedgerEntry.EntryType.CREDIT
                    ? "TRANSFER_IN" : "TRANSFER_OUT";

            CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                    .walletId(wallet.getId())
                    .userId(wallet.getUserId())
                    .entryType(entry.getEntryType().name())
                    .transactionType(transactionType)
                    .amountMinorUnits(entry.getAmountMinorUnits())
                    .currency(wallet.getCurrency().name())
                    .balanceAfter(entry.getBalanceAfter())
                    .transactionId(entry.getTransactionId())
                    .counterpartyWalletId(counterpartyWalletId)
                    .idempotencyKey(entry.getIdempotencyKey())
                    .description(entry.getDescription())
                    .build();

            ledgerClient.recordTransaction(request);
        } catch (Exception e) {
            log.error("Failed to record transfer to Ledger Service: {}", e.getMessage());
        }
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Send deposit notification to user.
     */
    private void sendDepositNotification(Wallet wallet, LedgerEntry entry) {
        try {
            String email = getUserEmail();
            String amount = formatAmount(entry.getAmountMinorUnits(), wallet.getCurrency());
            String newBalance = formatAmount(wallet.getBalanceMinorUnits(), wallet.getCurrency());

            notificationClient.notifyDeposit(
                    wallet.getUserId(),
                    email,
                    amount,
                    wallet.getCurrency().name(),
                    wallet.getId(),
                    entry.getTransactionId(),
                    newBalance
            );

            // Check for large transaction
            if (entry.getAmountMinorUnits() >= largeTransactionThreshold) {
                notificationClient.notifyLargeTransaction(
                        wallet.getUserId(), email, amount,
                        wallet.getCurrency().name(), "DEPOSIT", entry.getTransactionId()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send deposit notification: {}", e.getMessage());
        }
    }

    /**
     * Send withdrawal notification to user.
     */
    private void sendWithdrawalNotification(Wallet wallet, LedgerEntry entry) {
        try {
            String email = getUserEmail();
            String amount = formatAmount(entry.getAmountMinorUnits(), wallet.getCurrency());
            String newBalance = formatAmount(wallet.getBalanceMinorUnits(), wallet.getCurrency());

            notificationClient.notifyWithdrawal(
                    wallet.getUserId(),
                    email,
                    amount,
                    wallet.getCurrency().name(),
                    wallet.getId(),
                    entry.getTransactionId(),
                    newBalance
            );

            // Check for large transaction
            if (entry.getAmountMinorUnits() >= largeTransactionThreshold) {
                notificationClient.notifyLargeTransaction(
                        wallet.getUserId(), email, amount,
                        wallet.getCurrency().name(), "WITHDRAWAL", entry.getTransactionId()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send withdrawal notification: {}", e.getMessage());
        }
    }

    /**
     * Send transfer notifications to both sender and receiver.
     */
    private void sendTransferNotifications(Wallet fromWallet, Wallet toWallet,
                                            LedgerEntry debitEntry, LedgerEntry creditEntry,
                                            Long amount, UUID transactionId) {
        try {
            String amountFormatted = formatAmount(amount, fromWallet.getCurrency());
            String currency = fromWallet.getCurrency().name();

            // Notify sender
            String senderEmail = getUserEmail(); // Current user is the sender
            String senderNewBalance = formatAmount(fromWallet.getBalanceMinorUnits(), fromWallet.getCurrency());
            notificationClient.notifyTransferSent(
                    fromWallet.getUserId(),
                    senderEmail,
                    amountFormatted,
                    currency,
                    toWallet.getId(),
                    transactionId,
                    senderNewBalance
            );

            // Notify receiver (we don't have their email, use placeholder)
            String receiverNewBalance = formatAmount(toWallet.getBalanceMinorUnits(), toWallet.getCurrency());
            notificationClient.notifyTransferReceived(
                    toWallet.getUserId(),
                    "user@example.com", // In production, fetch from Customer Service
                    amountFormatted,
                    currency,
                    fromWallet.getId(),
                    transactionId,
                    receiverNewBalance
            );

            // Check for large transaction (alert sender)
            if (amount >= largeTransactionThreshold) {
                notificationClient.notifyLargeTransaction(
                        fromWallet.getUserId(), senderEmail, amountFormatted,
                        currency, "TRANSFER", transactionId
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send transfer notifications: {}", e.getMessage());
        }
    }

    /**
     * Get current user's email from context.
     */
    private String getUserEmail() {
        String email = UserContext.getCurrentUserEmail();
        return email != null ? email : "user@example.com";
    }

    /**
     * Format amount from minor units to display string.
     */
    private String formatAmount(Long minorUnits, Wallet.Currency currency) {
        double amount = minorUnits / 100.0;
        String symbol = switch (currency) {
            case USD -> "$";
            case EUR -> "€";
            case UZS -> "UZS ";
        };
        return String.format("%s%.2f", symbol, amount);
    }
}
