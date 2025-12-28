package com.fintech.customerservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Address information")
public record AddressRequest(
        @Schema(description = "Street address", example = "123 Main St")
        String street,

        @Schema(description = "City", example = "New York")
        String city,

        @Schema(description = "State/Province", example = "NY")
        String state,

        @Schema(description = "Postal/ZIP code", example = "10001")
        String postalCode,

        @Schema(description = "Country", example = "USA")
        String country
) {}

