package com.fintech.customerservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Customer profile entity.
 * Contains user profile information separate from auth credentials.
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_customer_phone", columnList = "phoneNumber"),
        @Index(name = "idx_customer_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Links to the User in Auth Service.
     * One-to-one relationship.
     */
    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ======= Enums =======

    public enum KycStatus {
        NOT_STARTED,     // No KYC submitted
        PENDING,         // Documents submitted, awaiting review
        VERIFIED,        // KYC approved
        REJECTED,        // KYC rejected
        EXPIRED          // KYC needs renewal
    }

    public enum CustomerStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED
    }

    // ======= Lifecycle callbacks =======

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ======= Helper methods =======

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isKycVerified() {
        return kycStatus == KycStatus.VERIFIED;
    }
}

