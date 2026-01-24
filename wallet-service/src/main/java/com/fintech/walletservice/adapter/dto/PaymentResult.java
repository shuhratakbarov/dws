package com.fintech.walletservice.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result object for deposit operations from payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    /**
     * Whether the payment was successful.
     */
    private boolean success;

    /**
     * Provider's transaction ID for tracking.
     */
    private String transactionId;

    /**
     * Amount processed in minor units.
     */
    private Long amount;

    /**
     * Error code from the provider (null if successful).
     * Common codes:
     * - INSUFFICIENT_FUNDS
     * - CARD_DECLINED
     * - INVALID_CARD
     * - EXPIRED_CARD
     * - FRAUD_SUSPECTED
     * - PROCESSING_ERROR
     */
    private String errorCode;

    /**
     * Human-readable error message (null if successful).
     */
    private String errorMessage;

    /**
     * Provider name that processed the transaction.
     */
    private String providerName;

    /**
     * Provider's status (PENDING, COMPLETED, FAILED).
     */
    private String status;

    /**
     * Create a successful payment result.
     */
    public static PaymentResult success(String transactionId, Long amount, String providerName) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .amount(amount)
                .providerName(providerName)
                .status("COMPLETED")
                .build();
    }

    /**
     * Create a pending payment result (for async processing).
     */
    public static PaymentResult pending(String transactionId, Long amount, String providerName) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .amount(amount)
                .providerName(providerName)
                .status("PENDING")
                .build();
    }

    /**
     * Create a failed payment result.
     */
    public static PaymentResult failure(String errorCode, String errorMessage, String providerName) {
        return PaymentResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .providerName(providerName)
                .status("FAILED")
                .build();
    }
}
