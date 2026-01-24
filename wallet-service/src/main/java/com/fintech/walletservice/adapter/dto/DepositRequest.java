package com.fintech.walletservice.adapter.dto;

import com.fintech.walletservice.domain.Wallet.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request object for deposit operations through payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    /**
     * The wallet ID to deposit funds into.
     */
    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    /**
     * Amount in minor units (cents, tiyin, etc.).
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    /**
     * Currency of the deposit.
     */
    @NotNull(message = "Currency is required")
    private Currency currency;

    /**
     * Payment method token (card token, bank account token, etc.).
     * Format depends on the payment provider:
     * - Payme: Card token from checkout
     * - Click: Payment token from Click API
     * - Stripe: Payment method ID (pm_xxx)
     */
    @NotNull(message = "Payment method token is required")
    private String paymentMethodToken;

    /**
     * Optional description for the transaction.
     */
    private String description;

    /**
     * Idempotency key to prevent duplicate transactions.
     */
    private String idempotencyKey;

    /**
     * User ID who initiated the deposit.
     */
    private UUID userId;
}
