package com.fintech.walletservice.adapter.stripe;

import com.fintech.walletservice.adapter.PaymentProviderAdapter;
import com.fintech.walletservice.adapter.dto.*;
import com.fintech.walletservice.config.PaymentProviderConfig;
import com.fintech.walletservice.domain.Wallet.Currency;
import com.fintech.walletservice.service.WithdrawalWebhookHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payment adapter for Stripe (International payment provider).
 *
 * Production implementation would integrate with:
 * - Stripe Payment Intents API for deposits
 * - Stripe Payouts API for withdrawals
 * - Stripe webhook handling
 *
 * API Documentation: https://stripe.com/docs/api
 */
@Service
@Slf4j
public class StripeAdapter implements PaymentProviderAdapter {

    private static final String PROVIDER_NAME = "STRIPE";
    private static final List<Currency> SUPPORTED_CURRENCIES = List.of(
            Currency.USD,
            Currency.EUR
    );

    private final PaymentProviderConfig.StripeConfig config;
    private final WithdrawalWebhookHandler webhookHandler;

    public StripeAdapter(PaymentProviderConfig providerConfig, WithdrawalWebhookHandler webhookHandler) {
        this.config = providerConfig.getStripe();
        this.webhookHandler = webhookHandler;
        log.info("StripeAdapter initialized");
    }

    @Override
    public PaymentResult processDeposit(DepositRequest request) {
        log.info("Processing Stripe deposit: walletId={}, amount={} {}",
                request.getWalletId(), request.getAmount(), request.getCurrency());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PaymentResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Stripe supports USD and EUR currencies",
                    PROVIDER_NAME
            );
        }

        if (request.getAmount() < 50) { // Minimum 50 cents
            return PaymentResult.failure(
                    "AMOUNT_TOO_SMALL",
                    "Minimum deposit amount is 50 cents",
                    PROVIDER_NAME
            );
        }

        if (request.getPaymentMethodToken() == null ||
            !request.getPaymentMethodToken().startsWith("pm_")) {
            return PaymentResult.failure(
                    "INVALID_PAYMENT_METHOD",
                    "Valid Stripe payment method ID (pm_xxx) is required",
                    PROVIDER_NAME
            );
        }

        // ===== PRODUCTION: Replace with actual Stripe API call =====
        // Stripe.apiKey = config.getSecretKey();
        //
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(request.getAmount())
        //     .setCurrency(request.getCurrency().name().toLowerCase())
        //     .setPaymentMethod(request.getPaymentMethodToken())
        //     .setConfirm(true)
        //     .setMetadata(Map.of(
        //         "wallet_id", request.getWalletId().toString(),
        //         "idempotency_key", request.getIdempotencyKey()
        //     ))
        //     .build();
        //
        // RequestOptions requestOptions = RequestOptions.builder()
        //     .setIdempotencyKey(request.getIdempotencyKey())
        //     .build();
        //
        // PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);
        //
        // if ("succeeded".equals(paymentIntent.getStatus())) {
        //     return PaymentResult.success(paymentIntent.getId(), request.getAmount(), PROVIDER_NAME);
        // } else if ("requires_action".equals(paymentIntent.getStatus())) {
        //     return PaymentResult.pending(paymentIntent.getId(), request.getAmount(), PROVIDER_NAME);
        // } else {
        //     return PaymentResult.failure("PAYMENT_FAILED", "Payment was not successful", PROVIDER_NAME);
        // }
        // ============================================================

        // Mock implementation - returns success for demo
        String transactionId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        log.info("Stripe deposit successful: transactionId={}", transactionId);

        return PaymentResult.success(transactionId, request.getAmount(), PROVIDER_NAME);
    }

    @Override
    public PayoutResult processWithdrawal(WithdrawalRequest request) {
        log.info("Processing Stripe withdrawal: walletId={}, amount={} {}",
                request.getWalletId(), request.getAmount(), request.getCurrency());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PayoutResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Stripe supports USD and EUR currencies",
                    PROVIDER_NAME
            );
        }

        if (request.getAmount() < 100) { // Minimum 1 dollar/euro
            return PayoutResult.failure(
                    "AMOUNT_TOO_SMALL",
                    "Minimum withdrawal amount is 100 cents (1 USD/EUR)",
                    PROVIDER_NAME
            );
        }

        if (request.getDestinationToken() == null ||
            (!request.getDestinationToken().startsWith("ba_") &&
             !request.getDestinationToken().startsWith("card_"))) {
            return PayoutResult.failure(
                    "INVALID_DESTINATION",
                    "Valid Stripe bank account (ba_xxx) or card (card_xxx) is required",
                    PROVIDER_NAME
            );
        }

        // ===== PRODUCTION: Replace with actual Stripe API call =====
        // Stripe.apiKey = config.getSecretKey();
        //
        // PayoutCreateParams params = PayoutCreateParams.builder()
        //     .setAmount(request.getAmount())
        //     .setCurrency(request.getCurrency().name().toLowerCase())
        //     .setDestination(request.getDestinationToken())
        //     .setMetadata(Map.of(
        //         "wallet_id", request.getWalletId().toString(),
        //         "withdrawal_id", request.getWithdrawalId().toString()
        //     ))
        //     .build();
        //
        // RequestOptions requestOptions = RequestOptions.builder()
        //     .setIdempotencyKey(request.getIdempotencyKey())
        //     .build();
        //
        // Payout payout = Payout.create(params, requestOptions);
        //
        // return PayoutResult.builder()
        //     .success(true)
        //     .payoutId(payout.getId())
        //     .estimatedArrival(payout.getArrivalDate().toString())
        //     .providerName(PROVIDER_NAME)
        //     .status(payout.getStatus())
        //     .build();
        // ============================================================

        // Mock implementation - returns success for demo
        String payoutId = "po_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        log.info("Stripe withdrawal initiated: payoutId={}", payoutId);

        return PayoutResult.success(payoutId, "1-2 business days", PROVIDER_NAME);
    }

    @Override
    public List<Currency> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean verifyWebhook(String signature, String payload) {
        if (signature == null || payload == null) {
            log.warn("Missing signature or payload for webhook verification");
            return false;
        }

        try {
            // Stripe webhook signature verification
            // Format: t=timestamp,v1=signature,v0=signature(deprecated)
            String[] parts = signature.split(",");
            String timestamp = null;
            String v1Signature = null;

            for (String part : parts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    v1Signature = part.substring(3);
                }
            }

            if (timestamp == null || v1Signature == null) {
                log.warn("Invalid Stripe webhook signature format");
                return false;
            }

            // Verify signature: HMAC-SHA256(timestamp.payload)
            String signedPayload = timestamp + "." + payload;
            String expectedSignature = computeHmacSha256(signedPayload, config.getWebhookSecret());

            boolean valid = v1Signature.equals(expectedSignature);

            if (!valid) {
                log.warn("Invalid Stripe webhook signature");
            }

            return valid;
        } catch (Exception e) {
            log.error("Error verifying Stripe webhook signature", e);
            return false;
        }
    }

    @Override
    public void handleWebhook(WebhookEvent event) {
        log.info("Handling Stripe webhook: type={}, externalId={}",
                event.getEventType(), event.getExternalId());

        switch (event.getEventType()) {
            case PAYMENT_SUCCESS -> {
                log.info("Stripe payment successful: {}", event.getExternalId());
                // Credit wallet - typically handled via webhook for async confirmations
            }
            case PAYMENT_FAILED -> {
                log.warn("Stripe payment failed: {} - {}",
                        event.getExternalId(), event.getErrorMessage());
            }
            case PAYOUT_SUCCESS -> {
                if (event.getInternalId() != null) {
                    webhookHandler.handlePayoutSuccess(
                            event.getInternalId(),
                            event.getExternalId()
                    );
                }
            }
            case PAYOUT_FAILED -> {
                if (event.getInternalId() != null) {
                    webhookHandler.handlePayoutFailure(
                            event.getInternalId(),
                            event.getErrorMessage()
                    );
                }
            }
            case CHARGEBACK, DISPUTE_OPENED -> {
                log.error("Stripe dispute/chargeback: {} - requires manual handling",
                        event.getExternalId());
                // TODO: Freeze funds, notify admin
            }
            default -> log.info("Unhandled Stripe webhook event type: {}", event.getEventType());
        }
    }

    /**
     * Parse raw Stripe webhook payload into WebhookEvent.
     */
    public WebhookEvent parseWebhook(String payload) {
        // ===== PRODUCTION: Parse actual Stripe webhook format =====
        // Event stripeEvent = Event.GSON.fromJson(payload, Event.class);
        //
        // WebhookEvent.EventType eventType = switch (stripeEvent.getType()) {
        //     case "payment_intent.succeeded" -> WebhookEvent.EventType.PAYMENT_SUCCESS;
        //     case "payment_intent.payment_failed" -> WebhookEvent.EventType.PAYMENT_FAILED;
        //     case "payout.paid" -> WebhookEvent.EventType.PAYOUT_SUCCESS;
        //     case "payout.failed" -> WebhookEvent.EventType.PAYOUT_FAILED;
        //     case "charge.dispute.created" -> WebhookEvent.EventType.DISPUTE_OPENED;
        //     default -> WebhookEvent.EventType.UNKNOWN;
        // };
        //
        // StripeObject stripeObject = stripeEvent.getDataObjectDeserializer()
        //     .getObject().orElseThrow();
        //
        // return WebhookEvent.builder()
        //     .eventType(eventType)
        //     .externalId(((HasId) stripeObject).getId())
        //     .internalId(extractInternalId(stripeObject))
        //     .timestamp(Instant.ofEpochSecond(stripeEvent.getCreated()))
        //     .providerName(PROVIDER_NAME)
        //     .build();
        // ============================================================

        // Mock implementation
        return WebhookEvent.builder()
                .eventType(WebhookEvent.EventType.PAYMENT_SUCCESS)
                .externalId("pi_mock_" + System.currentTimeMillis())
                .timestamp(Instant.now())
                .providerName(PROVIDER_NAME)
                .build();
    }

    private String computeHmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
