package com.fintech.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for the API Gateway.
 *
 * For production, use Redis-based rate limiting for distributed systems.
 * This implementation is suitable for single-instance deployments.
 */
@Component
@Slf4j
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    // Store: key -> (count, windowStart)
    private final Map<String, RateLimitEntry> rateLimitCache = new ConcurrentHashMap<>();

    public RateLimitingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientKey = resolveClientKey(exchange);

            if (!isAllowed(clientKey, config)) {
                log.warn("Rate limit exceeded for: {}", clientKey);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-RateLimit-Retry-After",
                        String.valueOf(config.getWindowSizeSeconds()));
                return exchange.getResponse().setComplete();
            }

            // Add rate limit headers
            RateLimitEntry entry = rateLimitCache.get(clientKey);
            if (entry != null) {
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                        String.valueOf(config.getRequestsPerWindow()));
                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                        String.valueOf(Math.max(0, config.getRequestsPerWindow() - entry.count.get())));
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Resolve client key for rate limiting.
     * Priority: User ID > IP Address
     */
    private String resolveClientKey(org.springframework.web.server.ServerWebExchange exchange) {
        // Try to get user ID from header (set by AuthenticationFilter)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Fall back to IP address
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    /**
     * Check if request is allowed and increment counter.
     */
    private synchronized boolean isAllowed(String clientKey, Config config) {
        Instant now = Instant.now();

        RateLimitEntry entry = rateLimitCache.compute(clientKey, (key, existing) -> {
            if (existing == null || isWindowExpired(existing, config.getWindowSizeSeconds())) {
                // New window
                return new RateLimitEntry(now, new AtomicInteger(1));
            } else {
                // Existing window - increment
                existing.count.incrementAndGet();
                return existing;
            }
        });

        boolean allowed = entry.count.get() <= config.getRequestsPerWindow();

        // Cleanup old entries periodically (simple approach)
        if (rateLimitCache.size() > 10000) {
            cleanupExpiredEntries(config.getWindowSizeSeconds());
        }

        return allowed;
    }

    private boolean isWindowExpired(RateLimitEntry entry, int windowSizeSeconds) {
        return Instant.now().isAfter(entry.windowStart.plusSeconds(windowSizeSeconds));
    }

    private void cleanupExpiredEntries(int windowSizeSeconds) {
        Instant now = Instant.now();
        rateLimitCache.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().windowStart.plusSeconds(windowSizeSeconds)));
    }

    /**
     * Configuration for rate limiting.
     */
    public static class Config {
        private int requestsPerWindow = 100;  // Default: 100 requests
        private int windowSizeSeconds = 60;   // Default: per minute

        public int getRequestsPerWindow() {
            return requestsPerWindow;
        }

        public void setRequestsPerWindow(int requestsPerWindow) {
            this.requestsPerWindow = requestsPerWindow;
        }

        public int getWindowSizeSeconds() {
            return windowSizeSeconds;
        }

        public void setWindowSizeSeconds(int windowSizeSeconds) {
            this.windowSizeSeconds = windowSizeSeconds;
        }
    }

    /**
     * Entry for tracking rate limit state.
     */
    private static class RateLimitEntry {
        final Instant windowStart;
        final AtomicInteger count;

        RateLimitEntry(Instant windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

