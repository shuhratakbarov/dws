package com.fintech.walletservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for communicating with Auth Service.
 * Used to look up user IDs by email for transfers.
 */
@Component
@Slf4j
public class AuthClient {

    private final WebClient webClient;
    private final boolean enabled;

    public AuthClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl,
            @Value("${services.auth.enabled:true}") boolean enabled
    ) {
        this.webClient = webClientBuilder
                .baseUrl(authServiceUrl)
                .build();
        this.enabled = enabled;
        log.info("AuthClient initialized: url={}, enabled={}", authServiceUrl, enabled);
    }

    /**
     * Look up user ID by email.
     * Returns Optional.empty() if user not found or service unavailable.
     */
    public Optional<UUID> findUserIdByEmail(String email) {
        if (!enabled) {
            log.debug("Auth integration disabled");
            return Optional.empty();
        }

        try {
            UserLookupResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/auth/users/lookup")
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .bodyToMono(UserLookupResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.userId != null) {
                log.debug("Found user ID {} for email {}", response.userId, email);
                return Optional.of(response.userId);
            }
            return Optional.empty();

        } catch (WebClientResponseException.NotFound e) {
            log.debug("User not found for email: {}", email);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to lookup user by email: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record UserLookupResponse(UUID userId, String email) {}
}
