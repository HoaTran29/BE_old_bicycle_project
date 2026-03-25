package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.NotificationResponseDTO;
import com.backend.old_bicycle_project.entity.Notification;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.NotificationRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final ZoneId NOTIFICATION_STORAGE_ZONE_ID = ZoneOffset.UTC;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void sendNotification(UUID userId, String title, String content, NotificationType type, String metadata) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .metadata(metadata)
                .createdAt(java.time.LocalDateTime.now(ZoneOffset.UTC))
                .build();

        notification = notificationRepository.save(notification);
        
        NotificationResponseDTO responseDTO = mapToDTO(notification);

        // Push via WebSocket to user's private queue
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                responseDTO
        );
    }

    @Override
    public Page<NotificationResponseDTO> getUserNotifications(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .metadata(notification.getMetadata())
                .createdAt(notification.getCreatedAt() != null
                        ? notification.getCreatedAt().atZone(NOTIFICATION_STORAGE_ZONE_ID).toOffsetDateTime()
                        : null)
                .build();
    }
}
