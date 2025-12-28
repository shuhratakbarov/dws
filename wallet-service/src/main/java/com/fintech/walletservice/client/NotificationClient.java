package com.fintech.walletservice.client;

import com.fintech.walletservice.client.dto.SendNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for communicating with Notification Service.
 * Uses async fire-and-forget pattern - notification failures don't block transactions.
 */
@Component
@Slf4j
public class NotificationClient {

    private final WebClient webClient;
    private final boolean enabled;

    public NotificationClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.notification.url:http://localhost:8085}") String notificationServiceUrl,
            @Value("${services.notification.enabled:true}") boolean enabled
    ) {
        this.webClient = webClientBuilder
                .baseUrl(notificationServiceUrl)
                .build();
        this.enabled = enabled;
        log.info("NotificationClient initialized: url={}, enabled={}", notificationServiceUrl, enabled);
    }

    /**
     * Send deposit notification.
     */
    public void notifyDeposit(UUID userId, String email, String amount, String currency,
                               UUID walletId, UUID transactionId, String newBalance) {
        if (!enabled) return;

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "DEPOSIT_RECEIVED",
                List.of("EMAIL"),
                email,
                "Deposit Received - Digital Wallet",
                null,
                "deposit-received",
                Map.of(
                        "amount", amount,
                        "currency", currency,
                        "walletId", walletId.toString(),
                        "transactionId", transactionId.toString(),
                        "newBalance", newBalance,
                        "date", java.time.LocalDate.now().toString()
                ),
                transactionId,
                "TRANSACTION"
        );

        sendAsync(request);
    }

    /**
     * Send withdrawal notification.
     */
    public void notifyWithdrawal(UUID userId, String email, String amount, String currency,
                                  UUID walletId, UUID transactionId, String newBalance) {
        if (!enabled) return;

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "WITHDRAWAL_COMPLETED",
                List.of("EMAIL"),
                email,
                "Withdrawal Completed - Digital Wallet",
                "<h2>Withdrawal Completed</h2>" +
                        "<p>Amount: <strong>" + amount + " " + currency + "</strong></p>" +
                        "<p>New Balance: " + newBalance + "</p>" +
                        "<p>Transaction ID: " + transactionId + "</p>",
                null,
                null,
                transactionId,
                "TRANSACTION"
        );

        sendAsync(request);
    }

    /**
     * Send transfer notification to sender.
     */
    public void notifyTransferSent(UUID userId, String email, String amount, String currency,
                                    UUID toWalletId, UUID transactionId, String newBalance) {
        if (!enabled) return;

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "TRANSFER_SENT",
                List.of("EMAIL"),
                email,
                "Transfer Sent - Digital Wallet",
                null,
                "transfer",
                Map.of(
                        "direction", "sent",
                        "amount", amount,
                        "currency", currency,
                        "counterpartyWalletId", toWalletId.toString(),
                        "transactionId", transactionId.toString(),
                        "newBalance", newBalance,
                        "date", java.time.LocalDate.now().toString()
                ),
                transactionId,
                "TRANSACTION"
        );

        sendAsync(request);
    }

    /**
     * Send transfer notification to receiver.
     */
    public void notifyTransferReceived(UUID userId, String email, String amount, String currency,
                                        UUID fromWalletId, UUID transactionId, String newBalance) {
        if (!enabled) return;

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "TRANSFER_RECEIVED",
                List.of("EMAIL"),
                email,
                "Transfer Received - Digital Wallet",
                null,
                "transfer",
                Map.of(
                        "direction", "received",
                        "amount", amount,
                        "currency", currency,
                        "counterpartyWalletId", fromWalletId.toString(),
                        "transactionId", transactionId.toString(),
                        "newBalance", newBalance,
                        "date", java.time.LocalDate.now().toString()
                ),
                transactionId,
                "TRANSACTION"
        );

        sendAsync(request);
    }

    /**
     * Send large transaction alert.
     */
    public void notifyLargeTransaction(UUID userId, String email, String amount, String currency,
                                        String transactionType, UUID transactionId) {
        if (!enabled) return;

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "LARGE_TRANSACTION",
                List.of("EMAIL", "SMS"),
                email,
                "⚠️ Large Transaction Alert - Digital Wallet",
                "<h2>Large Transaction Detected</h2>" +
                        "<p>A large " + transactionType.toLowerCase() + " of <strong>" +
                        amount + " " + currency + "</strong> was processed on your account.</p>" +
                        "<p>If you did not authorize this transaction, please contact support immediately.</p>",
                null,
                null,
                transactionId,
                "TRANSACTION"
        );

        sendAsync(request);
    }

    /**
     * Send notification asynchronously.
     */
    private void sendAsync(SendNotificationRequest request) {
        try {
            webClient.post()
                    .uri("/api/v1/notifications/send-async")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(3))
                    .subscribe(
                            response -> log.debug("Notification sent: {}", request.notificationType()),
                            error -> log.warn("Failed to send notification: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Error sending notification: {}", e.getMessage());
        }
    }
}

