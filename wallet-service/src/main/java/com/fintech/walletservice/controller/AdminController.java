package com.fintech.walletservice.controller;

import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.dto.response.ErrorResponse;
import com.fintech.walletservice.dto.response.WalletResponse;
import com.fintech.walletservice.job.BalanceReconciliationJob;
import com.fintech.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {

    private final BalanceReconciliationJob reconciliationJob;
    private final WalletService walletService;

    @Operation(
            summary = "Trigger balance reconciliation",
            description = """
                    Manually runs the balance reconciliation job that compares wallet balances 
                    against ledger entries. Useful for auditing or detecting data inconsistencies.
                    
                    In production, this runs automatically on a schedule (e.g., daily at midnight).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconciliation completed"),
            @ApiResponse(responseCode = "500", description = "Reconciliation failed")
    })
    @PostMapping("/reconcile")
    public ResponseEntity<BalanceReconciliationJob.ReconciliationReport> triggerReconciliation() {
        BalanceReconciliationJob.ReconciliationReport report =
                reconciliationJob.reconcileAllWalletsManually();
        return ResponseEntity.ok(report);
    }

    @Operation(
            summary = "Freeze a wallet",
            description = "Freezes a wallet, preventing all transactions. Used for fraud prevention or compliance."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet frozen successfully",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<WalletResponse> freezeWallet(
            @Parameter(description = "Wallet UUID to freeze")
            @PathVariable UUID walletId
    ) {
        Wallet wallet = walletService.freezeWallet(walletId);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @Operation(
            summary = "Unfreeze a wallet",
            description = "Unfreezes a previously frozen wallet, allowing transactions again."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet unfrozen successfully",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<WalletResponse> unfreezeWallet(
            @Parameter(description = "Wallet UUID to unfreeze")
            @PathVariable UUID walletId
    ) {
        Wallet wallet = walletService.unfreezeWallet(walletId);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }
}
