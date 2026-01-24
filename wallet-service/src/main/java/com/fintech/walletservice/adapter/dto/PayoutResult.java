package com.fintech.walletservice.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result object for withdrawal/payout operations from payment providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResult {

    /**
     * Whether the payout request was accepted.
     * Note: This doesn't mean funds have arrived - payouts are often async.
     */
    private boolean success;

    /**
     * Provider's payout ID for tracking.
     */
    private String payoutId;

    /**
     * Estimated arrival time/date of funds.
     * Format varies by provider (e.g., "2024-01-15", "1-3 business days").
     */
    private String estimatedArrival;

    /**
     * Human-readable error message (null if successful).
     */
    private String errorMessage;

    /**
     * Error code from the provider (null if successful).
     */
    private String errorCode;

    /**
     * Provider name that processed the payout.
     */
    private String providerName;

    /**
     * Payout status (PENDING, PROCESSING, COMPLETED, FAILED).
     */
    private String status;

    /**
     * Amount processed in minor units.
     */
    private Long amount;

    /**
     * Create a successful payout result.
     */
    public static PayoutResult success(String payoutId, String estimatedArrival, String providerName) {
        return PayoutResult.builder()
                .success(true)
                .payoutId(payoutId)
                .estimatedArrival(estimatedArrival)
                .providerName(providerName)
                .status("PROCESSING")
                .build();
    }

    /**
     * Create a pending payout result.
     */
    public static PayoutResult pending(String payoutId, String providerName) {
        return PayoutResult.builder()
                .success(true)
                .payoutId(payoutId)
                .providerName(providerName)
                .status("PENDING")
                .estimatedArrival("Pending review")
                .build();
    }

    /**
     * Create a failed payout result.
     */
    public static PayoutResult failure(String errorCode, String errorMessage, String providerName) {
        return PayoutResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .providerName(providerName)
                .status("FAILED")
                .build();
    }
}
