package com.fintech.authservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Authentication response with tokens")
public record AuthResponse(
        @Schema(description = "JWT access token for API calls")
        String accessToken,

        @Schema(description = "Refresh token for obtaining new access tokens")
        String refreshToken,

        @Schema(description = "Access token expiration time in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "User email", example = "john.doe@example.com")
        String email,

        @Schema(description = "User's first name", example = "John")
        String firstName,

        @Schema(description = "User's last name", example = "Doe")
        String lastName
) {}

