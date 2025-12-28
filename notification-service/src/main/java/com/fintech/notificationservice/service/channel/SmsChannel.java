package com.fintech.notificationservice.service.channel;

import com.fintech.notificationservice.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS notification channel.
 *
 * This is a MOCK implementation for learning purposes.
 * In production, you would integrate with:
 * - Twilio
 * - AWS SNS
 * - Vonage (Nexmo)
 * - MessageBird
 */
@Component
@Slf4j
public class SmsChannel implements NotificationChannel {

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    @Override
    public Notification.Channel getChannel() {
        return Notification.Channel.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        if (!enabled) {
            log.debug("SMS channel disabled, skipping: {}", notification.getId());
            return true;
        }

        // In production, integrate with SMS provider here
        // Example with Twilio:
        // twilioClient.messages().create(
        //     new PhoneNumber(notification.getRecipient()),
        //     new PhoneNumber(fromNumber),
        //     notification.getContent()
        // );

        log.info("[MOCK SMS] To: {}, Message: {}",
                notification.getRecipient(),
                truncate(notification.getContent(), 160));

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

