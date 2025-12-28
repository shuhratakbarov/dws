package com.fintech.customerservice.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embedded address component for Customer.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null) sb.append(street).append(", ");
        if (city != null) sb.append(city).append(", ");
        if (state != null) sb.append(state).append(" ");
        if (postalCode != null) sb.append(postalCode).append(", ");
        if (country != null) sb.append(country);
        return sb.toString().replaceAll(", $", "");
    }
}

