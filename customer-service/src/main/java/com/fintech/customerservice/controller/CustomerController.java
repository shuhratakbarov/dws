package com.fintech.customerservice.controller;

import com.fintech.customerservice.domain.Customer;
import com.fintech.customerservice.dto.request.CreateCustomerRequest;
import com.fintech.customerservice.dto.request.UpdateCustomerRequest;
import com.fintech.customerservice.dto.response.CustomerResponse;
import com.fintech.customerservice.dto.response.ErrorResponse;
import com.fintech.customerservice.service.CustomerService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(
            summary = "Create customer profile",
            description = "Creates a new customer profile linked to a user account"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer created",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Customer already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer));
    }

    @Operation(summary = "Get customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer UUID")
            @PathVariable UUID customerId
    ) {
        Customer customer = customerService.getCustomer(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @Operation(summary = "Get customer by user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomerResponse> getCustomerByUserId(
            @Parameter(description = "User UUID from Auth Service")
            @PathVariable UUID userId
    ) {
        Customer customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @Operation(summary = "Update customer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        Customer customer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @Operation(summary = "Get my profile (from JWT)")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        if (userIdHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(userIdHeader);
        Customer customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @Operation(summary = "Update my profile (from JWT)")
    @PutMapping("/me")
    public ResponseEntity<CustomerResponse> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        if (userIdHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(userIdHeader);
        Customer customer = customerService.getCustomerByUserId(userId);
        Customer updated = customerService.updateCustomer(customer.getId(), request);
        return ResponseEntity.ok(CustomerResponse.from(updated));
    }
}

