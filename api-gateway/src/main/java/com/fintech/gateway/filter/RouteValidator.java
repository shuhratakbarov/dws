package com.fintech.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Validates which routes require authentication and which are public.
 */
@Component
public class RouteValidator {

    /**
     * List of endpoints that don't require authentication.
     */
    private static final List<String> OPEN_ENDPOINTS = List.of(
            // Auth endpoints
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",

            // Health checks
            "/actuator/health",
            "/actuator/info",

            // Swagger/OpenAPI (if exposed through gateway)
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    );

    /**
     * Predicate to check if request is for a secured endpoint.
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

    /**
     * Check if the request is for an open (public) endpoint.
     */
    public boolean isOpenEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return OPEN_ENDPOINTS.stream().anyMatch(path::contains);
    }
}

