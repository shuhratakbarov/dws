package com.fintech.customerservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create a customer profile")
public record CreateCustomerRequest(
        @Schema(description = "User ID from Auth Service", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "User ID is required")
        UUID userId,

        @Schema(description = "First name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Phone number", example = "+1234567890")
        String phoneNumber
) {}

