package com.fintech.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * User notification preferences.
 * Allows users to opt-in/out of specific notification types and channels.
 */
@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "notificationType", "channel"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Notification.NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Notification.Channel channel;

    /**
     * Whether this notification type is enabled for this channel.
     */
    @Builder.Default
    private boolean enabled = true;
}

