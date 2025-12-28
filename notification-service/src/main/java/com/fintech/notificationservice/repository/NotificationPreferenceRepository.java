package com.fintech.notificationservice.repository;

import com.fintech.notificationservice.domain.Notification;
import com.fintech.notificationservice.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
            UUID userId, Notification.NotificationType type, Notification.Channel channel);

    List<NotificationPreference> findByUserIdAndEnabled(UUID userId, boolean enabled);
}

