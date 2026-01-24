package com.fintech.walletservice.adapter;

import com.fintech.walletservice.adapter.dto.*;
import com.fintech.walletservice.domain.Wallet.Currency;

import java.util.List;

/**
 * Adapter interface for payment providers.
 * Implements the Adapter Pattern to provide a unified interface
 * for different payment providers (Payme, Click, Stripe, etc.).
 *
 * Each adapter handles the specific API contract of its provider
 * while exposing a consistent interface to the application.
 */
public interface PaymentProviderAdapter {

    /**
     * Process a deposit request through the payment provider.
     *
     * @param request Deposit request with amount, currency, and payment method token
     * @return PaymentResult indicating success/failure and transaction details
     */
    PaymentResult processDeposit(DepositRequest request);

    /**
     * Process a withdrawal/payout request through the payment provider.
     *
     * @param request Withdrawal request with amount, currency, and destination token
     * @return PayoutResult indicating success/failure and payout details
     */
    PayoutResult processWithdrawal(WithdrawalRequest request);

    /**
     * Get list of currencies supported by this payment provider.
     *
     * @return List of supported currencies
     */
    List<Currency> getSupportedCurrencies();

    /**
     * Get the unique identifier/name of this payment provider.
     *
     * @return Provider name (e.g., "PAYME", "CLICK", "STRIPE")
     */
    String getProviderName();

    /**
     * Verify webhook signature to ensure the callback is authentic.
     *
     * @param signature The signature from the webhook header
     * @param payload The raw request body
     * @return true if signature is valid, false otherwise
     */
    boolean verifyWebhook(String signature, String payload);

    /**
     * Handle incoming webhook event from the payment provider.
     * Updates transaction status based on provider callbacks.
     *
     * @param event The parsed webhook event
     */
    void handleWebhook(WebhookEvent event);

    /**
     * Check if this adapter supports the given currency.
     *
     * @param currency The currency to check
     * @return true if supported, false otherwise
     */
    default boolean supportsCurrency(Currency currency) {
        return getSupportedCurrencies().contains(currency);
    }
}
