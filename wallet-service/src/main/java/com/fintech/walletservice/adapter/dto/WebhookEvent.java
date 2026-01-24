package com.fintech.walletservice.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic webhook event from payment providers.
 * Each provider's raw webhook is parsed into this unified format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    /**
     * Webhook event type.
     */
    private EventType eventType;

    /**
     * Provider's transaction/payout ID.
     */
    private String externalId;

    /**
     * Our internal transaction/withdrawal ID (if provided by webhook).
     */
    private UUID internalId;

    /**
     * New status from the provider.
     */
    private String status;

    /**
     * Amount in minor units (if applicable).
     */
    private Long amount;

    /**
     * Error code (if failed).
     */
    private String errorCode;

    /**
     * Error message (if failed).
     */
    private String errorMessage;

    /**
     * Timestamp of the event.
     */
    private Instant timestamp;

    /**
     * Raw payload for debugging/logging.
     */
    private Map<String, Object> rawPayload;

    /**
     * Provider name that sent the webhook.
     */
    private String providerName;

    /**
     * Types of webhook events we handle.
     */
    public enum EventType {
        // Deposit events
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_PENDING,
        PAYMENT_CANCELLED,

        // Withdrawal/Payout events
        PAYOUT_SUCCESS,
        PAYOUT_FAILED,
        PAYOUT_PROCESSING,
        PAYOUT_CANCELLED,

        // Refund events
        REFUND_SUCCESS,
        REFUND_FAILED,

        // Other events
        CHARGEBACK,
        DISPUTE_OPENED,
        DISPUTE_CLOSED,

        // Unknown event type
        UNKNOWN
    }
}
