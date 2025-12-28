package com.fintech.walletservice.controller;

import com.fintech.walletservice.domain.LedgerEntry;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.dto.request.CreateWalletForMeRequest;
import com.fintech.walletservice.dto.request.CreateWalletRequest;
import com.fintech.walletservice.dto.request.TransactionRequest;
import com.fintech.walletservice.dto.request.TransferRequest;
import com.fintech.walletservice.dto.response.ErrorResponse;
import com.fintech.walletservice.dto.response.LedgerEntryResponse;
import com.fintech.walletservice.dto.response.TransactionResponse;
import com.fintech.walletservice.dto.response.TransferResponse;
import com.fintech.walletservice.dto.response.WalletResponse;
import com.fintech.walletservice.security.UserContext;
import com.fintech.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management operations")
public class WalletController {

    private final WalletService walletService;

    @Operation(
            summary = "Create a new wallet",
            description = "Creates a new wallet for a user in a specific currency. Each user can have only one wallet per currency."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Wallet created successfully",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Wallet already exists for this user and currency",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request.userId(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WalletResponse.from(wallet));
    }

    @Operation(
            summary = "Create wallet for current user",
            description = "Creates a wallet for the authenticated user. User ID is extracted from JWT token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Wallet created successfully",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Wallet already exists for this currency",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/me")
    public ResponseEntity<WalletResponse> createWalletForMe(@Valid @RequestBody CreateWalletForMeRequest request) {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Wallet wallet = walletService.createWallet(userId, request.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WalletResponse.from(wallet));
    }

    @Operation(
            summary = "Get my wallets",
            description = "Returns all wallets owned by the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of wallets"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<List<WalletResponse>> getMyWallets() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<WalletResponse> wallets = walletService.getWalletsByUser(userId)
                .stream()
                .map(WalletResponse::from)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    @Operation(
            summary = "Deposit funds",
            description = "Adds funds to a wallet. Amount must be in minor units (cents). " +
                    "Use idempotencyKey to safely retry failed requests."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit successful",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Wallet is frozen or closed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Wallet UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID walletId,
            @Valid @RequestBody TransactionRequest request
    ) {
        LedgerEntry entry = walletService.deposit(
                walletId,
                request.amountMinorUnits(),
                request.idempotencyKey(),
                request.description()
        );
        return ResponseEntity.ok(TransactionResponse.from(entry));
    }

    @Operation(
            summary = "Withdraw funds",
            description = "Removes funds from a wallet. Fails if insufficient balance. " +
                    "Amount must be in minor units (cents)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Withdrawal successful",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Wallet is frozen or closed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Wallet UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID walletId,
            @Valid @RequestBody TransactionRequest request
    ) {
        LedgerEntry entry = walletService.withdraw(
                walletId,
                request.amountMinorUnits(),
                request.idempotencyKey(),
                request.description()
        );
        return ResponseEntity.ok(TransactionResponse.from(entry));
    }

    @Operation(
            summary = "Transfer between wallets",
            description = "Atomically transfers funds between two wallets. " +
                    "Both wallets must have the same currency. " +
                    "The operation is all-or-nothing: either both wallets are updated, or neither is."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "One or both wallets not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient funds in source wallet",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Currency mismatch or wallet not active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        UUID transactionId = walletService.transfer(
                request.fromWalletId(),
                request.toWalletId(),
                request.amountMinorUnits(),
                request.idempotencyKey(),
                request.description()
        );

        return ResponseEntity.ok(new TransferResponse(
                transactionId,
                request.fromWalletId(),
                request.toWalletId(),
                request.amountMinorUnits(),
                "COMPLETED"
        ));
    }

    @Operation(
            summary = "Get wallet by ID",
            description = "Retrieves wallet details including current balance"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet found",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(
            @Parameter(description = "Wallet UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID walletId
    ) {
        Wallet wallet = walletService.getWallet(walletId);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @Operation(
            summary = "Get all wallets for a user",
            description = "Returns all wallets owned by the specified user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of wallets (may be empty)"
            )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<java.util.List<WalletResponse>> getWalletsByUser(
            @Parameter(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID userId
    ) {
        java.util.List<WalletResponse> wallets = walletService.getWalletsByUser(userId)
                .stream()
                .map(WalletResponse::from)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    @Operation(
            summary = "Get transaction history",
            description = "Returns paginated transaction history for a wallet, ordered by most recent first"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<org.springframework.data.domain.Page<LedgerEntryResponse>> getTransactionHistory(
            @Parameter(description = "Wallet UUID")
            @PathVariable UUID walletId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<LedgerEntryResponse> history = walletService
                .getTransactionHistory(walletId, pageable)
                .map(LedgerEntryResponse::from);
        return ResponseEntity.ok(history);
    }

}
