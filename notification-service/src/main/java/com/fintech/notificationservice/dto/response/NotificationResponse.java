package com.fintech.notificationservice.dto.response;

import com.fintech.notificationservice.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Notification response")
public record NotificationResponse(
        UUID id,
        UUID userId,
        String channel,
        String notificationType,
        String recipient,
        String subject,
        String status,
        int attempts,
        String errorMessage,
        UUID referenceId,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getChannel().name(),
                n.getNotificationType().name(),
                n.getRecipient(),
                n.getSubject(),
                n.getStatus().name(),
                n.getAttempts(),
                n.getErrorMessage(),
                n.getReferenceId(),
                n.getCreatedAt(),
                n.getSentAt()
        );
    }
}

