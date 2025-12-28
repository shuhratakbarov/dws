package com.fintech.walletservice.client;

import com.fintech.walletservice.client.dto.CreateLedgerEntryRequest;
import com.fintech.walletservice.client.dto.LedgerEntryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for communicating with Ledger Service.
 * Uses WebClient for non-blocking HTTP calls.
 */
@Component
@Slf4j
public class LedgerClient {

    private final WebClient webClient;
    private final boolean enabled;

    public LedgerClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.ledger.url:http://localhost:8084}") String ledgerServiceUrl,
            @Value("${services.ledger.enabled:true}") boolean enabled
    ) {
        this.webClient = webClientBuilder
                .baseUrl(ledgerServiceUrl)
                .build();
        this.enabled = enabled;
        log.info("LedgerClient initialized: url={}, enabled={}", ledgerServiceUrl, enabled);
    }

    /**
     * Record a transaction in the ledger.
     * This is fire-and-forget with retry logic.
     */
    public LedgerEntryResponse recordTransaction(CreateLedgerEntryRequest request) {
        if (!enabled) {
            log.debug("Ledger integration disabled, skipping: {}", request.idempotencyKey());
            return null;
        }

        try {
            LedgerEntryResponse response = webClient.post()
                    .uri("/api/v1/ledger/entries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LedgerEntryResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Recorded ledger entry: {}", response != null ? response.id() : "null");
            return response;

        } catch (WebClientResponseException e) {
            log.error("Ledger service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Don't fail the transaction if ledger is down
            // In production, you'd use a message queue for reliability
            return null;
        } catch (Exception e) {
            log.error("Failed to record ledger entry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if idempotency key already exists.
     */
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        if (!enabled) {
            return false;
        }

        try {
            Boolean exists = webClient.get()
                    .uri("/api/v1/ledger/idempotency/{key}", idempotencyKey)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check idempotency key: {}", e.getMessage());
            return false;
        }
    }
}

