package com.fintech.walletservice.service;

import com.fintech.walletservice.adapter.PaymentProviderAdapter;
import com.fintech.walletservice.adapter.click.ClickAdapter;
import com.fintech.walletservice.adapter.dto.*;
import com.fintech.walletservice.adapter.payme.PaymeAdapter;
import com.fintech.walletservice.adapter.stripe.StripeAdapter;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.domain.Wallet.Currency;
import com.fintech.walletservice.exception.PaymentProviderException;
import com.fintech.walletservice.exception.WalletNotFoundException;
import com.fintech.walletservice.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Routes payment requests to the appropriate payment provider adapter.
 *
 * Routing logic:
 * - UZS → Payme (primary) or Click (fallback)
 * - USD/EUR → Stripe
 *
 * Provides unified interface for deposits and withdrawals
 * while handling provider-specific routing logic.
 */
@Service
@Slf4j
public class PaymentRoutingService {

    private final Map<String, PaymentProviderAdapter> adapters;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    // Provider preferences by currency
    private static final Map<Currency, List<String>> PROVIDER_PREFERENCES = Map.of(
            Currency.UZS, List.of("PAYME", "CLICK"),
            Currency.USD, List.of("STRIPE"),
            Currency.EUR, List.of("STRIPE")
    );

    public PaymentRoutingService(
            PaymeAdapter paymeAdapter,
            ClickAdapter clickAdapter,
            StripeAdapter stripeAdapter,
            WalletRepository walletRepository,
            WalletService walletService
    ) {
        this.adapters = new HashMap<>();
        this.adapters.put("PAYME", paymeAdapter);
        this.adapters.put("CLICK", clickAdapter);
        this.adapters.put("STRIPE", stripeAdapter);
        this.walletRepository = walletRepository;
        this.walletService = walletService;

        log.info("PaymentRoutingService initialized with {} adapters", adapters.size());
    }

    /**
     * Select the appropriate payment provider for a currency.
     * Returns primary provider, use getAlternativeProvider for fallback.
     *
     * @param currency The currency to process
     * @return The primary payment provider adapter
     * @throws PaymentProviderException if no provider supports the currency
     */
    public PaymentProviderAdapter selectProvider(Currency currency) {
        List<String> providers = PROVIDER_PREFERENCES.get(currency);

        if (providers == null || providers.isEmpty()) {
            throw new PaymentProviderException(
                    "No payment provider configured for currency: " + currency
            );
        }

        String primaryProvider = providers.get(0);
        PaymentProviderAdapter adapter = adapters.get(primaryProvider);

        if (adapter == null) {
            throw new PaymentProviderException(
                    "Payment provider not found: " + primaryProvider
            );
        }

        log.debug("Selected provider {} for currency {}", primaryProvider, currency);
        return adapter;
    }

    /**
     * Get alternative (fallback) provider for a currency.
     *
     * @param currency The currency
     * @param excludeProvider Provider to exclude (failed primary)
     * @return Optional fallback provider
     */
    public Optional<PaymentProviderAdapter> getAlternativeProvider(
            Currency currency,
            String excludeProvider
    ) {
        List<String> providers = PROVIDER_PREFERENCES.get(currency);

        if (providers == null) {
            return Optional.empty();
        }

        return providers.stream()
                .filter(p -> !p.equals(excludeProvider))
                .map(adapters::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * Process a deposit through the appropriate payment provider.
     * Automatically routes to correct provider based on currency.
     *
     * @param request Deposit request
     * @return Payment result from provider
     */
    @Transactional
    public PaymentResult processDeposit(DepositRequest request) {
        log.info("Processing deposit: walletId={}, amount={} {}",
                request.getWalletId(), request.getAmount(), request.getCurrency());

        // Validate wallet exists
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found: " + request.getWalletId()
                ));

        // Ensure currency matches
        if (wallet.getCurrency() != request.getCurrency()) {
            return PaymentResult.failure(
                    "CURRENCY_MISMATCH",
                    "Deposit currency " + request.getCurrency() +
                    " does not match wallet currency " + wallet.getCurrency(),
                    "ROUTER"
            );
        }

        // Select provider
        PaymentProviderAdapter adapter = selectProvider(request.getCurrency());

        // Process with primary provider
        PaymentResult result = adapter.processDeposit(request);

        // Try fallback if primary fails
        if (!result.isSuccess()) {
            Optional<PaymentProviderAdapter> fallback = getAlternativeProvider(
                    request.getCurrency(),
                    adapter.getProviderName()
            );

            if (fallback.isPresent()) {
                log.warn("Primary provider {} failed, trying fallback {}",
                        adapter.getProviderName(), fallback.get().getProviderName());
                result = fallback.get().processDeposit(request);
            }
        }

        // If successful, credit the wallet
        if (result.isSuccess() && "COMPLETED".equals(result.getStatus())) {
            String idempotencyKey = request.getIdempotencyKey() != null
                    ? request.getIdempotencyKey()
                    : UUID.randomUUID().toString();

            walletService.deposit(
                    request.getWalletId(),
                    request.getAmount(),
                    idempotencyKey,
                    "Deposit via " + result.getProviderName() +
                    " - Ref: " + result.getTransactionId()
            );

            log.info("Deposit completed: walletId={}, amount={}, provider={}",
                    request.getWalletId(), request.getAmount(), result.getProviderName());
        }

        return result;
    }

    /**
     * Process a withdrawal through the appropriate payment provider.
     * Automatically routes to correct provider based on currency.
     *
     * @param request Withdrawal request
     * @return Payout result from provider
     */
    @Transactional
    public PayoutResult processWithdrawal(WithdrawalRequest request) {
        log.info("Processing withdrawal: walletId={}, amount={} {}",
                request.getWalletId(), request.getAmount(), request.getCurrency());

        // Validate wallet exists
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found: " + request.getWalletId()
                ));

        // Ensure currency matches
        if (wallet.getCurrency() != request.getCurrency()) {
            return PayoutResult.failure(
                    "CURRENCY_MISMATCH",
                    "Withdrawal currency " + request.getCurrency() +
                    " does not match wallet currency " + wallet.getCurrency(),
                    "ROUTER"
            );
        }

        // Check sufficient balance
        if (wallet.getBalanceMinorUnits() < request.getAmount()) {
            return PayoutResult.failure(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient balance. Available: " + wallet.getBalanceMinorUnits() +
                    ", Requested: " + request.getAmount(),
                    "ROUTER"
            );
        }

        // Generate withdrawal ID if not provided
        UUID withdrawalId = request.getWithdrawalId() != null
                ? request.getWithdrawalId()
                : UUID.randomUUID();
        request.setWithdrawalId(withdrawalId);

        // Reserve funds (debit from wallet) before calling provider
        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : withdrawalId.toString() + "-withdrawal";

        walletService.withdraw(
                request.getWalletId(),
                request.getAmount(),
                idempotencyKey,
                "Withdrawal pending - " + request.getDescription()
        );

        // Select provider and process
        PaymentProviderAdapter adapter = selectProvider(request.getCurrency());
        PayoutResult result = adapter.processWithdrawal(request);

        // Try fallback if primary fails
        if (!result.isSuccess()) {
            Optional<PaymentProviderAdapter> fallback = getAlternativeProvider(
                    request.getCurrency(),
                    adapter.getProviderName()
            );

            if (fallback.isPresent()) {
                log.warn("Primary provider {} failed, trying fallback {}",
                        adapter.getProviderName(), fallback.get().getProviderName());
                result = fallback.get().processWithdrawal(request);
            }
        }

        // If provider rejected immediately, refund
        if (!result.isSuccess()) {
            log.warn("Withdrawal failed, refunding: {}", result.getErrorMessage());
            walletService.deposit(
                    request.getWalletId(),
                    request.getAmount(),
                    withdrawalId.toString() + "-refund",
                    "Refund: Withdrawal failed - " + result.getErrorMessage()
            );
        } else {
            log.info("Withdrawal initiated: walletId={}, amount={}, provider={}, payoutId={}",
                    request.getWalletId(), request.getAmount(),
                    result.getProviderName(), result.getPayoutId());
        }

        return result;
    }

    /**
     * Get all available payment providers.
     *
     * @return Map of provider name to adapter
     */
    public Map<String, PaymentProviderAdapter> getAllProviders() {
        return Collections.unmodifiableMap(adapters);
    }

    /**
     * Get providers that support a specific currency.
     *
     * @param currency The currency to check
     * @return List of provider names supporting the currency
     */
    public List<String> getProvidersForCurrency(Currency currency) {
        return PROVIDER_PREFERENCES.getOrDefault(currency, List.of());
    }

    /**
     * Check if a currency is supported by any provider.
     *
     * @param currency The currency to check
     * @return true if supported
     */
    public boolean isCurrencySupported(Currency currency) {
        return PROVIDER_PREFERENCES.containsKey(currency) &&
               !PROVIDER_PREFERENCES.get(currency).isEmpty();
    }
}
