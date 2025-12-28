package com.fintech.customerservice.dto.response;

import com.fintech.customerservice.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Customer profile response")
public record CustomerResponse(
        @Schema(description = "Customer ID")
        UUID id,

        @Schema(description = "User ID (from Auth Service)")
        UUID userId,

        @Schema(description = "First name")
        String firstName,

        @Schema(description = "Last name")
        String lastName,

        @Schema(description = "Full name")
        String fullName,

        @Schema(description = "Phone number")
        String phoneNumber,

        @Schema(description = "Date of birth")
        LocalDate dateOfBirth,

        @Schema(description = "Address")
        AddressResponse address,

        @Schema(description = "KYC verification status")
        String kycStatus,

        @Schema(description = "Account status")
        String status,

        @Schema(description = "When profile was created")
        Instant createdAt
) {
    public static CustomerResponse from(Customer customer) {
        AddressResponse addr = null;
        if (customer.getAddress() != null) {
            addr = new AddressResponse(
                    customer.getAddress().getStreet(),
                    customer.getAddress().getCity(),
                    customer.getAddress().getState(),
                    customer.getAddress().getPostalCode(),
                    customer.getAddress().getCountry()
            );
        }

        return new CustomerResponse(
                customer.getId(),
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFullName(),
                customer.getPhoneNumber(),
                customer.getDateOfBirth(),
                addr,
                customer.getKycStatus().name(),
                customer.getStatus().name(),
                customer.getCreatedAt()
        );
    }
}

