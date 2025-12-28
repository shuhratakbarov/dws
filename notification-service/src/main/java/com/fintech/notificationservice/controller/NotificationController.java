package com.fintech.notificationservice.controller;

import com.fintech.notificationservice.domain.Notification;
import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.dto.response.ErrorResponse;
import com.fintech.notificationservice.dto.response.NotificationResponse;
import com.fintech.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Multi-channel notification operations")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Send notification",
            description = "Send notification through specified channels (EMAIL, SMS, PUSH)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification(s) queued"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/send")
    public ResponseEntity<List<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        List<Notification> notifications = notificationService.send(request);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Send notification async",
            description = "Queue notification for async delivery (fire-and-forget)"
    )
    @PostMapping("/send-async")
    public ResponseEntity<Void> sendNotificationAsync(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        notificationService.sendAsync(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Get user notification history")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<NotificationResponse> history = notificationService
                .getUserNotifications(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get notification by ID")
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID notificationId
    ) {
        Notification notification = notificationService.getNotification(notificationId);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @Operation(
            summary = "Retry failed notifications",
            description = "Retry sending notifications that previously failed (admin)"
    )
    @PostMapping("/retry-failed")
    public ResponseEntity<Integer> retryFailed(
            @RequestParam(defaultValue = "3") int maxAttempts
    ) {
        int retried = notificationService.retryFailed(maxAttempts);
        return ResponseEntity.ok(retried);
    }
}

