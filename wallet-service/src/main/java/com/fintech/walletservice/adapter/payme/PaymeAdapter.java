package com.fintech.walletservice.adapter.payme;

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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Payment adapter for Payme (Uzbekistan payment provider).
 *
 * Production implementation would integrate with:
 * - Payme Checkout API for deposits
 * - Payme Business API for payouts
 * - Payme webhook handling
 *
 * API Documentation: https://developer.paycom.uz/
 */
@Service
@Slf4j
public class PaymeAdapter implements PaymentProviderAdapter {

    private static final String PROVIDER_NAME = "PAYME";
    private static final List<Currency> SUPPORTED_CURRENCIES = List.of(Currency.UZS);

    private final PaymentProviderConfig.PaymeConfig config;
    private final WithdrawalWebhookHandler webhookHandler;

    public PaymeAdapter(PaymentProviderConfig providerConfig, WithdrawalWebhookHandler webhookHandler) {
        this.config = providerConfig.getPayme();
        this.webhookHandler = webhookHandler;
        log.info("PaymeAdapter initialized with merchant ID: {}", config.getMerchantId());
    }

    @Override
    public PaymentResult processDeposit(DepositRequest request) {
        log.info("Processing Payme deposit: walletId={}, amount={} UZS",
                request.getWalletId(), request.getAmount());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PaymentResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Payme only supports UZS currency",
                    PROVIDER_NAME
            );
        }

        if (request.getAmount() < 100) { // Minimum 100 tiyin = 1 UZS
            return PaymentResult.failure(
                    "AMOUNT_TOO_SMALL",
                    "Minimum deposit amount is 100 tiyin (1 UZS)",
                    PROVIDER_NAME
            );
        }

        if (request.getPaymentMethodToken() == null || request.getPaymentMethodToken().isBlank()) {
            return PaymentResult.failure(
                    "INVALID_TOKEN",
                    "Payment method token is required",
                    PROVIDER_NAME
            );
        }

        // ===== PRODUCTION: Replace with actual Payme API call =====
        // PaymeCreateTransactionRequest paymeRequest = PaymeCreateTransactionRequest.builder()
        //     .id(UUID.randomUUID().toString())
        //     .method("receipts.create")
        //     .params(Map.of(
        //         "amount", request.getAmount(),
        //         "account", Map.of("wallet_id", request.getWalletId().toString())
        //     ))
        //     .build();
        //
        // PaymeResponse response = restTemplate.postForObject(
        //     config.getApiUrl(),
        //     paymeRequest,
        //     PaymeResponse.class
        // );
        // ============================================================

        // Mock implementation - returns success for demo
        String transactionId = "payme_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Payme deposit successful: transactionId={}", transactionId);

        return PaymentResult.success(transactionId, request.getAmount(), PROVIDER_NAME);
    }

    @Override
    public PayoutResult processWithdrawal(WithdrawalRequest request) {
        log.info("Processing Payme withdrawal: walletId={}, amount={} UZS",
                request.getWalletId(), request.getAmount());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PayoutResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Payme only supports UZS currency",
                    PROVIDER_NAME
            );
        }

        if (request.getAmount() < 100000) { // Minimum withdrawal 1000 UZS
            return PayoutResult.failure(
                    "AMOUNT_TOO_SMALL",
                    "Minimum withdrawal amount is 100,000 tiyin (1,000 UZS)",
                    PROVIDER_NAME
            );
        }

        if (request.getDestinationToken() == null || request.getDestinationToken().isBlank()) {
            return PayoutResult.failure(
                    "INVALID_DESTINATION",
                    "Destination card token is required",
                    PROVIDER_NAME
            );
        }

        // ===== PRODUCTION: Replace with actual Payme API call =====
        // PaymePayoutRequest paymeRequest = PaymePayoutRequest.builder()
        //     .id(UUID.randomUUID().toString())
        //     .method("payouts.create")
        //     .params(Map.of(
        //         "amount", request.getAmount(),
        //         "card_token", request.getDestinationToken(),
        //         "reference_id", request.getWithdrawalId().toString()
        //     ))
        //     .build();
        //
        // PaymePayoutResponse response = restTemplate.postForObject(
        //     config.getApiUrl(),
        //     paymeRequest,
        //     PaymePayoutResponse.class
        // );
        // ============================================================

        // Mock implementation - returns success for demo
        String payoutId = "payme_payout_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Payme withdrawal initiated: payoutId={}", payoutId);

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
            // Payme uses Base64(merchantId:secretKey) for Basic auth
            // For webhooks, verify HMAC-SHA256 signature
            String expectedSignature = computeHmacSha256(payload, config.getSecretKey());
            boolean valid = signature.equals(expectedSignature);

            if (!valid) {
                log.warn("Invalid Payme webhook signature");
            }

            return valid;
        } catch (Exception e) {
            log.error("Error verifying Payme webhook signature", e);
            return false;
        }
    }

    @Override
    public void handleWebhook(WebhookEvent event) {
        log.info("Handling Payme webhook: type={}, externalId={}",
                event.getEventType(), event.getExternalId());

        switch (event.getEventType()) {
            case PAYMENT_SUCCESS -> {
                log.info("Payme payment successful: {}", event.getExternalId());
                // Credit wallet is typically handled in the initial flow
            }
            case PAYMENT_FAILED -> {
                log.warn("Payme payment failed: {} - {}",
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
            default -> log.info("Unhandled Payme webhook event type: {}", event.getEventType());
        }
    }

    /**
     * Parse raw Payme webhook payload into WebhookEvent.
     * This would be called by the webhook controller.
     */
    public WebhookEvent parseWebhook(String payload) {
        // ===== PRODUCTION: Parse actual Payme webhook format =====
        // PaymeWebhookPayload payme = objectMapper.readValue(payload, PaymeWebhookPayload.class);
        // return WebhookEvent.builder()
        //     .eventType(mapPaymeEvent(payme.getMethod()))
        //     .externalId(payme.getParams().getTransaction())
        //     .internalId(UUID.fromString(payme.getParams().getAccount().get("withdrawal_id")))
        //     .status(payme.getParams().getState().toString())
        //     .amount(payme.getParams().getAmount())
        //     .timestamp(Instant.now())
        //     .providerName(PROVIDER_NAME)
        //     .build();
        // ============================================================

        // Mock implementation
        return WebhookEvent.builder()
                .eventType(WebhookEvent.EventType.PAYMENT_SUCCESS)
                .externalId("payme_mock_" + System.currentTimeMillis())
                .timestamp(Instant.now())
                .providerName(PROVIDER_NAME)
                .build();
    }

    private String computeHmacSha256(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
