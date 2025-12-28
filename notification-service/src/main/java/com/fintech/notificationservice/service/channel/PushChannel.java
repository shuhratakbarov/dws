package com.fintech.notificationservice.service.channel;

import com.fintech.notificationservice.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Push notification channel.
 *
 * This is a MOCK implementation for learning purposes.
 * In production, you would integrate with:
 * - Firebase Cloud Messaging (FCM) for Android/iOS/Web
 * - Apple Push Notification Service (APNs) for iOS
 * - Amazon SNS for cross-platform
 */
@Component
@Slf4j
public class PushChannel implements NotificationChannel {

    @Value("${notification.push.enabled:false}")
    private boolean enabled;

    @Override
    public Notification.Channel getChannel() {
        return Notification.Channel.PUSH;
    }

    @Override
    public boolean send(Notification notification) {
        if (!enabled) {
            log.debug("Push channel disabled, skipping: {}", notification.getId());
            return true;
        }

        // In production, integrate with FCM here:
        // FirebaseMessaging.getInstance().send(
        //     Message.builder()
        //         .setToken(notification.getRecipient()) // Device token
        //         .setNotification(Notification.builder()
        //             .setTitle(notification.getSubject())
        //             .setBody(notification.getContent())
        //             .build())
        //         .build()
        // );

        log.info("[MOCK PUSH] To device: {}, Title: {}, Body: {}",
                truncate(notification.getRecipient(), 20),
                notification.getSubject(),
                truncate(notification.getContent(), 100));

        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}

