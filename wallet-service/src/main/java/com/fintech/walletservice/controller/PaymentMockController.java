package com.fintech.walletservice.controller;

import com.fintech.walletservice.adapter.dto.*;
import com.fintech.walletservice.domain.Wallet.Currency;
import com.fintech.walletservice.service.PaymentRoutingService;
import com.fintech.walletservice.service.WithdrawalWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mock controller for simulating payment provider operations.
 *
 * ⚠️ FOR DEVELOPMENT/TESTING ONLY - DELETE IN PRODUCTION ⚠️
 *
 * This controller provides endpoints to simulate:
 * - Deposit success/failure scenarios
 * - Webhook callbacks from payment providers
 * - Withdrawal completion flows
 *
 * All randomness, delays, and mock logic is ONLY in this controller.
 * The actual adapters remain clean and production-ready.
 */
@RestController
@RequestMapping("/api/v1/mock")
@RequiredArgsConstructor
@Slf4j
//@Profile("!prod") // Disabled in production profile
@Tag(name = "Payment Mock", description = "Mock endpoints for testing payment flows (DEV ONLY)")
public class PaymentMockController {

    private final PaymentRoutingService paymentRoutingService;
    private final WithdrawalWebhookHandler webhookHandler;

    private final Random random = new Random();

    // ==================== PAYME MOCK ENDPOINTS ====================

    @PostMapping("/payme/deposit")
    @Operation(summary = "Simulate Payme deposit",
               description = "Simulates a Payme deposit. Use cardToken 'fail' to simulate failure.")
    public ResponseEntity<PaymentResult> mockPaymeDeposit(
            @RequestParam UUID walletId,
            @RequestParam Long amount,
            @RequestParam(defaultValue = "valid_card_token") String cardToken,
            @RequestParam(required = false) String idempotencyKey
    ) {
        log.info("[MOCK] Payme deposit: walletId={}, amount={}, cardToken={}",
                walletId, amount, cardToken);

        // Simulate processing delay
        simulateDelay(500, 1500);

        // Simulate failure scenarios based on cardToken
        if ("fail".equalsIgnoreCase(cardToken)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "CARD_DECLINED",
                    "Card was declined by issuing bank",
                    "PAYME"
            ));
        }

        if ("expired".equalsIgnoreCase(cardToken)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "EXPIRED_CARD",
                    "Card has expired",
                    "PAYME"
            ));
        }

        if ("fraud".equalsIgnoreCase(cardToken)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "FRAUD_SUSPECTED",
                    "Transaction flagged for fraud review",
                    "PAYME"
            ));
        }

        // Random failure (10% chance)
        if (random.nextInt(100) < 10) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "PROCESSING_ERROR",
                    "Random processing error for testing",
                    "PAYME"
            ));
        }

        // Process through routing service
        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(amount)
                .currency(Currency.UZS)
                .paymentMethodToken(cardToken)
                .idempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .description("Mock Payme deposit")
                .build();

        PaymentResult result = paymentRoutingService.processDeposit(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/payme/webhook")
    @Operation(summary = "Simulate Payme webhook",
               description = "Simulates a Payme webhook callback for withdrawal completion")
    public ResponseEntity<Map<String, Object>> mockPaymeWebhook(
            @RequestParam UUID withdrawalId,
            @RequestParam(defaultValue = "SUCCESS") String status,
            @RequestParam(required = false) String errorMessage
    ) {
        log.info("[MOCK] Payme webhook: withdrawalId={}, status={}", withdrawalId, status);

        // Simulate webhook processing delay
        simulateDelay(100, 500);

        String externalId = "payme_payout_" + UUID.randomUUID().toString().substring(0, 8);

        switch (status.toUpperCase()) {
            case "SUCCESS" -> webhookHandler.handlePayoutSuccess(withdrawalId, externalId);
            case "FAILED" -> webhookHandler.handlePayoutFailure(
                    withdrawalId,
                    errorMessage != null ? errorMessage : "Payment provider declined"
            );
            case "PROCESSING" -> webhookHandler.handlePayoutProcessing(withdrawalId, externalId);
            case "CANCELLED" -> webhookHandler.handlePayoutCancelled(withdrawalId, "User cancelled");
        }

        return ResponseEntity.ok(Map.of(
                "received", true,
                "withdrawalId", withdrawalId,
                "status", status,
                "externalId", externalId
        ));
    }

    // ==================== CLICK MOCK ENDPOINTS ====================

    @PostMapping("/click/deposit")
    @Operation(summary = "Simulate Click deposit",
               description = "Simulates a Click deposit. Use token 'fail' to simulate failure.")
    public ResponseEntity<PaymentResult> mockClickDeposit(
            @RequestParam UUID walletId,
            @RequestParam Long amount,
            @RequestParam(defaultValue = "valid_click_token") String token,
            @RequestParam(required = false) String idempotencyKey
    ) {
        log.info("[MOCK] Click deposit: walletId={}, amount={}, token={}",
                walletId, amount, token);

        simulateDelay(600, 2000);

        if ("fail".equalsIgnoreCase(token)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "PAYMENT_REJECTED",
                    "Click payment was rejected",
                    "CLICK"
            ));
        }

        if ("insufficient".equalsIgnoreCase(token)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient funds on payment source",
                    "CLICK"
            ));
        }

        // Random failure (5% chance for Click)
        if (random.nextInt(100) < 5) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "NETWORK_ERROR",
                    "Random network error for testing",
                    "CLICK"
            ));
        }

        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(amount)
                .currency(Currency.UZS)
                .paymentMethodToken(token)
                .idempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .description("Mock Click deposit")
                .build();

        PaymentResult result = paymentRoutingService.processDeposit(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/click/webhook")
    @Operation(summary = "Simulate Click webhook")
    public ResponseEntity<Map<String, Object>> mockClickWebhook(
            @RequestParam UUID withdrawalId,
            @RequestParam(defaultValue = "SUCCESS") String status,
            @RequestParam(required = false) String errorMessage
    ) {
        log.info("[MOCK] Click webhook: withdrawalId={}, status={}", withdrawalId, status);

        simulateDelay(100, 300);

        String externalId = "click_payout_" + System.currentTimeMillis();

        switch (status.toUpperCase()) {
            case "SUCCESS" -> webhookHandler.handlePayoutSuccess(withdrawalId, externalId);
            case "FAILED" -> webhookHandler.handlePayoutFailure(
                    withdrawalId,
                    errorMessage != null ? errorMessage : "Click payout failed"
            );
        }

        return ResponseEntity.ok(Map.of(
                "received", true,
                "withdrawalId", withdrawalId,
                "status", status,
                "externalId", externalId
        ));
    }

    // ==================== STRIPE MOCK ENDPOINTS ====================

    @PostMapping("/stripe/deposit")
    @Operation(summary = "Simulate Stripe deposit",
               description = "Simulates a Stripe deposit. Use paymentMethodId 'pm_fail' to simulate failure.")
    public ResponseEntity<PaymentResult> mockStripeDeposit(
            @RequestParam UUID walletId,
            @RequestParam Long amount,
            @RequestParam(defaultValue = "USD") Currency currency,
            @RequestParam(defaultValue = "pm_valid_card") String paymentMethodId,
            @RequestParam(required = false) String idempotencyKey
    ) {
        log.info("[MOCK] Stripe deposit: walletId={}, amount={} {}, pm={}",
                walletId, amount, currency, paymentMethodId);

        simulateDelay(800, 2500);

        if ("pm_fail".equalsIgnoreCase(paymentMethodId)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "card_declined",
                    "Your card was declined",
                    "STRIPE"
            ));
        }

        if ("pm_expired".equalsIgnoreCase(paymentMethodId)) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "expired_card",
                    "Your card has expired",
                    "STRIPE"
            ));
        }

        if ("pm_3ds_required".equalsIgnoreCase(paymentMethodId)) {
            // Simulate 3D Secure requirement
            String transactionId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            return ResponseEntity.ok(PaymentResult.pending(transactionId, amount, "STRIPE"));
        }

        // Random failure (3% chance for Stripe)
        if (random.nextInt(100) < 3) {
            return ResponseEntity.ok(PaymentResult.failure(
                    "processing_error",
                    "An error occurred while processing your card",
                    "STRIPE"
            ));
        }

        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(amount)
                .currency(currency)
                .paymentMethodToken(paymentMethodId)
                .idempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .description("Mock Stripe deposit")
                .build();

        PaymentResult result = paymentRoutingService.processDeposit(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Simulate Stripe webhook")
    public ResponseEntity<Map<String, Object>> mockStripeWebhook(
            @RequestParam UUID withdrawalId,
            @RequestParam(defaultValue = "payout.paid") String eventType,
            @RequestParam(required = false) String failureMessage
    ) {
        log.info("[MOCK] Stripe webhook: withdrawalId={}, eventType={}", withdrawalId, eventType);

        simulateDelay(50, 200);

        String externalId = "po_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        switch (eventType) {
            case "payout.paid" -> webhookHandler.handlePayoutSuccess(withdrawalId, externalId);
            case "payout.failed" -> webhookHandler.handlePayoutFailure(
                    withdrawalId,
                    failureMessage != null ? failureMessage : "Payout failed"
            );
            case "payout.canceled" -> webhookHandler.handlePayoutCancelled(withdrawalId, "Payout was cancelled");
        }

        return ResponseEntity.ok(Map.of(
                "received", true,
                "withdrawalId", withdrawalId,
                "eventType", eventType,
                "externalId", externalId
        ));
    }

    // ==================== GENERIC MOCK ENDPOINTS ====================

    @PostMapping("/complete-withdrawal/{withdrawalId}")
    @Operation(summary = "Force complete a pending withdrawal",
               description = "Manually triggers withdrawal completion for testing")
    public ResponseEntity<Map<String, Object>> forceCompleteWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(required = false) String message
    ) {
        log.info("[MOCK] Force complete withdrawal: id={}, success={}", withdrawalId, success);

        simulateDelay(100, 500);

        String externalId = "manual_" + UUID.randomUUID().toString().substring(0, 8);

        if (success) {
            webhookHandler.handlePayoutSuccess(withdrawalId, externalId);
            return ResponseEntity.ok(Map.of(
                    "completed", true,
                    "withdrawalId", withdrawalId,
                    "externalId", externalId,
                    "message", "Withdrawal marked as completed"
            ));
        } else {
            webhookHandler.handlePayoutFailure(
                    withdrawalId,
                    message != null ? message : "Manually failed for testing"
            );
            return ResponseEntity.ok(Map.of(
                    "completed", false,
                    "withdrawalId", withdrawalId,
                    "message", "Withdrawal marked as failed, funds refunded"
            ));
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "List available payment providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        var providers = paymentRoutingService.getAllProviders();

        return ResponseEntity.ok(Map.of(
                "providers", providers.keySet(),
                "currencyRouting", Map.of(
                        "UZS", paymentRoutingService.getProvidersForCurrency(Currency.UZS),
                        "USD", paymentRoutingService.getProvidersForCurrency(Currency.USD),
                        "EUR", paymentRoutingService.getProvidersForCurrency(Currency.EUR)
                )
        ));
    }

    // ==================== HELPER METHODS ====================

    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + random.nextInt(maxMs - minMs);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
