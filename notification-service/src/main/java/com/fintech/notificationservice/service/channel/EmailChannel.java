package com.fintech.notificationservice.service.channel;

import com.fintech.notificationservice.domain.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Email notification channel using Spring Mail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean enabled;

    @Value("${notification.email.from:noreply@digitalwallet.com}")
    private String fromAddress;

    @Override
    public Notification.Channel getChannel() {
        return Notification.Channel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        if (!enabled) {
            log.debug("Email channel disabled, skipping: {}", notification.getId());
            return true; // Consider disabled as success
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject() != null
                    ? notification.getSubject()
                    : "Digital Wallet Notification");
            helper.setText(notification.getContent(), true); // HTML content

            mailSender.send(message);
            log.info("Email sent to {} for notification {}",
                    notification.getRecipient(), notification.getId());
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}",
                    notification.getRecipient(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

