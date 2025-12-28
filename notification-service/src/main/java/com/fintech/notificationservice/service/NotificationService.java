package com.fintech.notificationservice.service;

import com.fintech.notificationservice.domain.Notification;
import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.repository.NotificationRepository;
import com.fintech.notificationservice.service.channel.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core notification service that orchestrates sending through multiple channels.
 */
@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TemplateService templateService;
    private final Map<Notification.Channel, NotificationChannel> channelProviders;

    public NotificationService(
            NotificationRepository notificationRepository,
            TemplateService templateService,
            List<NotificationChannel> channels
    ) {
        this.notificationRepository = notificationRepository;
        this.templateService = templateService;
        // Index channel providers by their channel type
        this.channelProviders = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getChannel, c -> c));
        log.info("Initialized NotificationService with channels: {}", channelProviders.keySet());
    }

    /**
     * Send notification through specified channels.
     * If no channels specified, defaults to EMAIL.
     */
    @Transactional
    public List<Notification> send(SendNotificationRequest request) {
        List<Notification.Channel> channels = request.channels() != null && !request.channels().isEmpty()
                ? request.channels()
                : List.of(Notification.Channel.EMAIL); // Default to email

        String content = resolveContent(request);
        String subject = resolveSubject(request);

        return channels.stream()
                .map(channel -> createAndSend(request, channel, subject, content))
                .toList();
    }

    /**
     * Send notification asynchronously (fire-and-forget).
     */
    @Async
    @Transactional
    public void sendAsync(SendNotificationRequest request) {
        try {
            send(request);
        } catch (Exception e) {
            log.error("Async notification failed: {}", e.getMessage());
        }
    }

    /**
     * Create notification record and attempt to send.
     */
    private Notification createAndSend(
            SendNotificationRequest request,
            Notification.Channel channel,
            String subject,
            String content
    ) {
        Notification notification = Notification.builder()
                .userId(request.userId())
                .channel(channel)
                .notificationType(request.notificationType())
                .recipient(request.recipient() != null ? request.recipient() : "unknown@example.com")
                .subject(subject)
                .content(content)
                .referenceId(request.referenceId())
                .referenceType(request.referenceType())
                .status(Notification.Status.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // Attempt to send
        sendInternal(notification);

        return notification;
    }

    /**
     * Internal send logic with error handling.
     */
    private void sendInternal(Notification notification) {
        NotificationChannel provider = channelProviders.get(notification.getChannel());

        if (provider == null) {
            log.error("No provider for channel: {}", notification.getChannel());
            notification.markFailed("No provider configured for channel");
            notificationRepository.save(notification);
            return;
        }

        notification.incrementAttempts();

        try {
            boolean success = provider.send(notification);
            if (success) {
                notification.markSent();
            } else {
                notification.markFailed("Send returned false");
            }
        } catch (Exception e) {
            log.error("Error sending notification {}: {}", notification.getId(), e.getMessage());
            notification.markFailed(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    /**
     * Resolve content from template or raw content.
     */
    private String resolveContent(SendNotificationRequest request) {
        if (request.templateName() != null) {
            return templateService.render(
                    request.templateName(),
                    request.templateData() != null ? request.templateData() : Map.of()
            );
        }
        return request.content() != null ? request.content() : "";
    }

    /**
     * Resolve subject based on notification type.
     */
    private String resolveSubject(SendNotificationRequest request) {
        if (request.subject() != null) {
            return request.subject();
        }
        return switch (request.notificationType()) {
            case DEPOSIT_RECEIVED -> "Deposit Received - Digital Wallet";
            case WITHDRAWAL_COMPLETED -> "Withdrawal Completed - Digital Wallet";
            case TRANSFER_SENT -> "Transfer Sent - Digital Wallet";
            case TRANSFER_RECEIVED -> "Transfer Received - Digital Wallet";
            case LOGIN_ALERT -> "New Login Detected - Digital Wallet";
            case PASSWORD_CHANGED -> "Password Changed - Digital Wallet";
            case SUSPICIOUS_ACTIVITY -> "⚠️ Security Alert - Digital Wallet";
            case LOW_BALANCE -> "Low Balance Alert - Digital Wallet";
            case LARGE_TRANSACTION -> "Large Transaction Alert - Digital Wallet";
            case ACCOUNT_CREATED -> "Welcome to Digital Wallet!";
            case KYC_APPROVED -> "KYC Verification Approved";
            case KYC_REJECTED -> "KYC Verification Update";
            default -> "Notification - Digital Wallet";
        };
    }

    /**
     * Get notification history for user.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get notification by ID.
     */
    @Transactional(readOnly = true)
    public Notification getNotification(UUID id) {
        return notificationRepository.findById(id).orElse(null);
    }

    /**
     * Retry failed notifications (called by scheduled job).
     */
    @Transactional
    public int retryFailed(int maxAttempts) {
        List<Notification> failed = notificationRepository
                .findByStatusAndAttemptsLessThan(Notification.Status.FAILED, maxAttempts);

        int retried = 0;
        for (Notification notification : failed) {
            notification.setStatus(Notification.Status.PENDING);
            sendInternal(notification);
            retried++;
        }

        return retried;
    }
}

