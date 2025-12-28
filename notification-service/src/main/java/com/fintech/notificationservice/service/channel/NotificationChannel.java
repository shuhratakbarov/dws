package com.fintech.notificationservice.service.channel;

import com.fintech.notificationservice.domain.Notification;

/**
 * Interface for notification channel providers.
 * Each channel (Email, SMS, Push) implements this.
 */
public interface NotificationChannel {

    /**
     * Get the channel type this provider handles.
     */
    Notification.Channel getChannel();

    /**
     * Send notification through this channel.
     * @param notification The notification to send
     * @return true if sent successfully
     */
    boolean send(Notification notification);

    /**
     * Check if this channel is enabled/configured.
     */
    boolean isEnabled();
}

