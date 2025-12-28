package com.fintech.customerservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Address response")
public record AddressResponse(
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {}

