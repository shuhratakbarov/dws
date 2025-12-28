package com.fintech.notificationservice.dto.request;

import com.fintech.notificationservice.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Request to send a notification")
public record SendNotificationRequest(
        @Schema(description = "User ID to notify", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "User ID is required")
        UUID userId,

        @Schema(description = "Notification type", example = "DEPOSIT_RECEIVED")
        @NotNull(message = "Notification type is required")
        Notification.NotificationType notificationType,

        @Schema(description = "Channels to use (if empty, uses user preferences)", example = "[\"EMAIL\", \"PUSH\"]")
        List<Notification.Channel> channels,

        @Schema(description = "Recipient email/phone (if not using user's default)")
        String recipient,

        @Schema(description = "Subject for email or title for push")
        String subject,

        @Schema(description = "Message content (or use template)")
        String content,

        @Schema(description = "Template name to use instead of raw content", example = "deposit-received")
        String templateName,

        @Schema(description = "Template variables", example = "{\"amount\": \"$100.00\", \"walletId\": \"xxx\"}")
        Map<String, Object> templateData,

        @Schema(description = "Reference ID (e.g., transaction ID)")
        UUID referenceId,

        @Schema(description = "Reference type", example = "TRANSACTION")
        String referenceType
) {}

