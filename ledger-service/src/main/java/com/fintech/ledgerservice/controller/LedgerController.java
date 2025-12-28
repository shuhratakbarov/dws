package com.fintech.ledgerservice.controller;

import com.fintech.ledgerservice.domain.LedgerEntry;
import com.fintech.ledgerservice.dto.request.CreateLedgerEntryRequest;
import com.fintech.ledgerservice.dto.response.BalanceResponse;
import com.fintech.ledgerservice.dto.response.ErrorResponse;
import com.fintech.ledgerservice.dto.response.LedgerEntryResponse;
import com.fintech.ledgerservice.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Immutable transaction ledger operations")
public class LedgerController {

    private final LedgerService ledgerService;

    // ==================== Internal API (called by other services) ====================

    @Operation(
            summary = "Create ledger entry",
            description = "Creates an immutable ledger entry. Called by Wallet Service during transactions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entry created"),
            @ApiResponse(responseCode = "200", description = "Entry already exists (idempotent)"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/entries")
    public ResponseEntity<LedgerEntryResponse> createEntry(
            @Valid @RequestBody CreateLedgerEntryRequest request
    ) {
        LedgerEntry entry = ledgerService.createEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LedgerEntryResponse.from(entry));
    }

    // ==================== Query API ====================

    @Operation(summary = "Get ledger entry by ID")
    @GetMapping("/entries/{entryId}")
    public ResponseEntity<LedgerEntryResponse> getEntry(
            @PathVariable UUID entryId
    ) {
        LedgerEntry entry = ledgerService.getEntry(entryId);
        return ResponseEntity.ok(LedgerEntryResponse.from(entry));
    }

    @Operation(summary = "Get wallet transaction history")
    @GetMapping("/wallets/{walletId}/history")
    public ResponseEntity<Page<LedgerEntryResponse>> getWalletHistory(
            @Parameter(description = "Wallet UUID")
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntryResponse> history = ledgerService.getWalletHistory(walletId, pageable)
                .map(LedgerEntryResponse::from);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get user transaction history (all wallets)")
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<Page<LedgerEntryResponse>> getUserHistory(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntryResponse> history = ledgerService.getUserHistory(userId, pageable)
                .map(LedgerEntryResponse::from);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get all entries for a transaction")
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<List<LedgerEntryResponse>> getTransactionEntries(
            @Parameter(description = "Transaction UUID")
            @PathVariable UUID transactionId
    ) {
        List<LedgerEntryResponse> entries = ledgerService.getTransactionEntries(transactionId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
        return ResponseEntity.ok(entries);
    }

    @Operation(summary = "Calculate balance from ledger")
    @GetMapping("/wallets/{walletId}/balance")
    public ResponseEntity<BalanceResponse> calculateBalance(
            @PathVariable UUID walletId
    ) {
        BalanceResponse balance = ledgerService.calculateBalance(walletId);
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Check if idempotency key exists")
    @GetMapping("/idempotency/{key}")
    public ResponseEntity<Boolean> checkIdempotencyKey(
            @PathVariable String key
    ) {
        boolean exists = ledgerService.existsByIdempotencyKey(key);
        return ResponseEntity.ok(exists);
    }
}

