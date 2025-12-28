package com.fintech.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token refresh request")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token received during login")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}

