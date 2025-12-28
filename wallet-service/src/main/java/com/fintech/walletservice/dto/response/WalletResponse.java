package com.fintech.walletservice.dto.response;

import com.fintech.walletservice.domain.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Wallet information")
public record WalletResponse(
        @Schema(description = "Wallet unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Owner's user ID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(description = "Wallet currency", example = "USD")
        String currency,

        @Schema(description = "Current balance in minor units", example = "150000")
        Long balance,

        @Schema(description = "Wallet status", example = "ACTIVE", allowableValues = {"ACTIVE", "FROZEN", "CLOSED"})
        String status
) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.getId(), w.getUserId(), w.getCurrency().name(),
                w.getBalanceMinorUnits(), w.getStatus().name());
    }
}
