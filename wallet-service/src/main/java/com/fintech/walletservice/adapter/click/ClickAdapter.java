package com.fintech.walletservice.adapter.click;

import com.fintech.walletservice.adapter.PaymentProviderAdapter;
import com.fintech.walletservice.adapter.dto.*;
import com.fintech.walletservice.config.PaymentProviderConfig;
import com.fintech.walletservice.domain.Wallet.Currency;
import com.fintech.walletservice.service.WithdrawalWebhookHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payment adapter for Click (Uzbekistan payment provider).
 *
 * Production implementation would integrate with:
 * - Click API for payments
 * - Click SHOP-API for merchants
 * - Click webhook handling
 *
 * API Documentation: https://docs.click.uz/
 */
@Service
@Slf4j
public class ClickAdapter implements PaymentProviderAdapter {

    private static final String PROVIDER_NAME = "CLICK";
    private static final List<Currency> SUPPORTED_CURRENCIES = List.of(Currency.UZS);

    private final PaymentProviderConfig.ClickConfig config;
    private final WithdrawalWebhookHandler webhookHandler;

    public ClickAdapter(PaymentProviderConfig providerConfig, WithdrawalWebhookHandler webhookHandler) {
        this.config = providerConfig.getClick();
        this.webhookHandler = webhookHandler;
        log.info("ClickAdapter initialized with merchant ID: {}", config.getMerchantId());
    }

    @Override
    public PaymentResult processDeposit(DepositRequest request) {
        log.info("Processing Click deposit: walletId={}, amount={} UZS",
                request.getWalletId(), request.getAmount());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PaymentResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Click only supports UZS currency",
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

        // ===== PRODUCTION: Replace with actual Click API call =====
        // ClickPaymentRequest clickRequest = ClickPaymentRequest.builder()
        //     .serviceId(config.getServiceId())
        //     .merchantId(config.getMerchantId())
        //     .amount(request.getAmount() / 100) // Click uses sum, not tiyin
        //     .merchantTransId(request.getIdempotencyKey())
        //     .merchantPrepareId(request.getWalletId().toString())
        //     .build();
        //
        // String signTime = String.valueOf(System.currentTimeMillis());
        // String signString = computeClickSignature(clickRequest, signTime);
        //
        // HttpHeaders headers = new HttpHeaders();
        // headers.set("Auth", config.getMerchantUserId() + ":" + signString + ":" + signTime);
        //
        // ClickPaymentResponse response = restTemplate.postForObject(
        //     config.getApiUrl() + "/merchant/payment/create",
        //     new HttpEntity<>(clickRequest, headers),
        //     ClickPaymentResponse.class
        // );
        // ============================================================

        // Mock implementation - returns success for demo
        String transactionId = "click_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Click deposit successful: transactionId={}", transactionId);

        return PaymentResult.success(transactionId, request.getAmount(), PROVIDER_NAME);
    }

    @Override
    public PayoutResult processWithdrawal(WithdrawalRequest request) {
        log.info("Processing Click withdrawal: walletId={}, amount={} UZS",
                request.getWalletId(), request.getAmount());

        // Validate request
        if (!SUPPORTED_CURRENCIES.contains(request.getCurrency())) {
            return PayoutResult.failure(
                    "UNSUPPORTED_CURRENCY",
                    "Click only supports UZS currency",
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
                    "Destination card/account token is required",
                    PROVIDER_NAME
            );
        }

        // ===== PRODUCTION: Replace with actual Click API call =====
        // ClickPayoutRequest clickRequest = ClickPayoutRequest.builder()
        //     .serviceId(config.getServiceId())
        //     .merchantId(config.getMerchantId())
        //     .amount(request.getAmount() / 100)
        //     .cardNumber(request.getDestinationToken())
        //     .merchantTransId(request.getWithdrawalId().toString())
        //     .build();
        //
        // ClickPayoutResponse response = restTemplate.postForObject(
        //     config.getApiUrl() + "/merchant/payout/create",
        //     new HttpEntity<>(clickRequest, headers),
        //     ClickPayoutResponse.class
        // );
        // ============================================================

        // Mock implementation - returns success for demo
        String payoutId = "click_payout_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Click withdrawal initiated: payoutId={}", payoutId);

        return PayoutResult.success(payoutId, "1-3 business days", PROVIDER_NAME);
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
            // Click uses MD5 hash for signature verification
            // sign_string = click_trans_id + service_id + secret_key + merchant_trans_id + amount + action + sign_time
            String expectedSignature = computeClickSignature(payload);
            boolean valid = signature.equalsIgnoreCase(expectedSignature);

            if (!valid) {
                log.warn("Invalid Click webhook signature");
            }

            return valid;
        } catch (Exception e) {
            log.error("Error verifying Click webhook signature", e);
            return false;
        }
    }

    @Override
    public void handleWebhook(WebhookEvent event) {
        log.info("Handling Click webhook: type={}, externalId={}",
                event.getEventType(), event.getExternalId());

        switch (event.getEventType()) {
            case PAYMENT_SUCCESS -> {
                log.info("Click payment successful: {}", event.getExternalId());
                // Credit wallet is typically handled in the initial flow
            }
            case PAYMENT_FAILED -> {
                log.warn("Click payment failed: {} - {}",
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
            default -> log.info("Unhandled Click webhook event type: {}", event.getEventType());
        }
    }

    /**
     * Parse raw Click webhook payload into WebhookEvent.
     */
    public WebhookEvent parseWebhook(String payload) {
        // ===== PRODUCTION: Parse actual Click webhook format =====
        // ClickWebhookPayload click = objectMapper.readValue(payload, ClickWebhookPayload.class);
        // WebhookEvent.EventType eventType = switch (click.getAction()) {
        //     case 0 -> WebhookEvent.EventType.PAYMENT_PENDING;  // Prepare
        //     case 1 -> WebhookEvent.EventType.PAYMENT_SUCCESS;  // Complete
        //     default -> WebhookEvent.EventType.UNKNOWN;
        // };
        // return WebhookEvent.builder()
        //     .eventType(eventType)
        //     .externalId(click.getClickTransId().toString())
        //     .internalId(UUID.fromString(click.getMerchantTransId()))
        //     .amount(click.getAmount() * 100) // Convert sum to tiyin
        //     .timestamp(Instant.now())
        //     .providerName(PROVIDER_NAME)
        //     .build();
        // ============================================================

        // Mock implementation
        return WebhookEvent.builder()
                .eventType(WebhookEvent.EventType.PAYMENT_SUCCESS)
                .externalId("click_mock_" + System.currentTimeMillis())
                .timestamp(Instant.now())
                .providerName(PROVIDER_NAME)
                .build();
    }

    private String computeClickSignature(String data) throws NoSuchAlgorithmException {
        // Click uses MD5(click_trans_id + service_id + secret_key + ...)
        String signString = data + config.getSecretKey();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(signString.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
