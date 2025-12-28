package com.fintech.notificationservice.repository;

import com.fintech.notificationservice.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Notification> findByStatusAndAttemptsLessThan(Notification.Status status, int maxAttempts);

    List<Notification> findByUserIdAndNotificationTypeAndCreatedAtAfter(
            UUID userId, Notification.NotificationType type, Instant after);

    long countByUserIdAndStatus(UUID userId, Notification.Status status);
}

