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
 * Request object for withdrawal/payout operations through payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {

    /**
     * The wallet ID to withdraw funds from.
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
     * Currency of the withdrawal.
     */
    @NotNull(message = "Currency is required")
    private Currency currency;

    /**
     * Destination token (bank account, card, etc.).
     * Format depends on the payment provider:
     * - Payme: Card token for payout
     * - Click: Bank account token
     * - Stripe: Bank account or debit card ID
     */
    @NotNull(message = "Destination token is required")
    private String destinationToken;

    /**
     * Optional description for the transaction.
     */
    private String description;

    /**
     * Idempotency key to prevent duplicate transactions.
     */
    private String idempotencyKey;

    /**
     * User ID who initiated the withdrawal.
     */
    private UUID userId;

    /**
     * Internal withdrawal ID for tracking.
     */
    private UUID withdrawalId;
}
