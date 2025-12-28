package com.fintech.walletservice.dto.request;

import com.fintech.walletservice.domain.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new wallet (user ID from JWT token)")
public record CreateWalletForMeRequest(
        @Schema(description = "Currency for the wallet", example = "USD")
        @NotNull Wallet.Currency currency
) {}

