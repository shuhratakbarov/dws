package com.fintech.customerservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Request to update customer profile")
public record UpdateCustomerRequest(
        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        String lastName,

        @Schema(description = "Phone number", example = "+1234567890")
        String phoneNumber,

        @Schema(description = "Date of birth", example = "1990-01-15")
        LocalDate dateOfBirth,

        @Schema(description = "Address")
        AddressRequest address
) {}

