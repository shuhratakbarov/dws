package com.fintech.walletservice.dto.request;

import com.fintech.walletservice.domain.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create a new wallet")
public record CreateWalletRequest(
        @Schema(description = "User ID who will own this wallet", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID userId,

        @Schema(description = "Currency for the wallet", example = "USD")
        @NotNull Wallet.Currency currency
) {}
