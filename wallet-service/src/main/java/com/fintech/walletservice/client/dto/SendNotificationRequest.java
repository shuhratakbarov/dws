package com.fintech.walletservice.client.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for sending notifications.
 * Mirrors the Notification Service's SendNotificationRequest.
 */
public record SendNotificationRequest(
        UUID userId,
        String notificationType,
        List<String> channels,
        String recipient,
        String subject,
        String content,
        String templateName,
        Map<String, Object> templateData,
        UUID referenceId,
        String referenceType
) {}

