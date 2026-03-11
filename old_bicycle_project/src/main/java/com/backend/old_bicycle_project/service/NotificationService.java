package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.NotificationResponseDTO;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /**
     * Send a notification to a specific user
     */
    void sendNotification(UUID userId, String title, String content, NotificationType type, String metadata);

    /**
     * Get paginated notifications for a user
     */
    Page<NotificationResponseDTO> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Mark a specific notification as read
     */
    void markAsRead(UUID notificationId);

    /**
     * Mark all notifications as read for a user
     */
    void markAllAsRead(UUID userId);

    /**
     * Get unread notification count for a user
     */
    long getUnreadCount(UUID userId);
}
