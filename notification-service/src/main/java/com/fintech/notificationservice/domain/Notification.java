package com.fintech.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores notification history for audit and retry purposes.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "userId"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_type", columnList = "notificationType"),
        @Index(name = "idx_notification_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who receives the notification.
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * Channel: EMAIL, SMS, or PUSH
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    /**
     * Type of notification for categorization.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    /**
     * Recipient address (email, phone, or device token).
     */
    @Column(nullable = false)
    private String recipient;

    /**
     * Subject (for email) or title (for push).
     */
    private String subject;

    /**
     * Message content.
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Delivery status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Error message if delivery failed.
     */
    private String errorMessage;

    /**
     * Number of delivery attempts.
     */
    @Builder.Default
    private int attempts = 0;

    /**
     * Reference to related entity (e.g., transaction ID).
     */
    private UUID referenceId;

    /**
     * Reference type (e.g., "TRANSACTION", "WALLET").
     */
    private String referenceType;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant sentAt;

    // ======= Enums =======

    public enum Channel {
        EMAIL,
        SMS,
        PUSH
    }

    public enum NotificationType {
        // Transactional
        DEPOSIT_RECEIVED,
        WITHDRAWAL_COMPLETED,
        TRANSFER_SENT,
        TRANSFER_RECEIVED,

        // Security
        LOGIN_ALERT,
        PASSWORD_CHANGED,
        SUSPICIOUS_ACTIVITY,

        // Alerts
        LOW_BALANCE,
        LARGE_TRANSACTION,

        // Account
        ACCOUNT_CREATED,
        KYC_APPROVED,
        KYC_REJECTED,

        // Marketing
        PROMOTIONAL,

        // System
        SYSTEM_MAINTENANCE
    }

    public enum Status {
        PENDING,
        SENT,
        DELIVERED,
        FAILED,
        CANCELLED
    }

    // ======= Helper Methods =======

    public void markSent() {
        this.status = Status.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = Status.FAILED;
        this.errorMessage = error;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}

