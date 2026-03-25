package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.NotificationResponseDTO;
import com.backend.old_bicycle_project.entity.Notification;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.NotificationRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void sendNotificationPersistsUtcTimestampAndPushesToUserQueue() {
        User user = user("buyer@test.dev");
        UUID notificationId = UUID.randomUUID();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(notificationId);
            return notification;
        });

        notificationService.sendNotification(
                user.getId(),
                "Cập nhật order",
                "Order của bạn đã được xác nhận.",
                NotificationType.order,
                "{\"orderId\":\"abc\"}"
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        ArgumentCaptor<NotificationResponseDTO> responseCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(user.getId().toString()),
                eq("/queue/notifications"),
                responseCaptor.capture()
        );

        Notification persistedNotification = notificationCaptor.getValue();
        NotificationResponseDTO response = responseCaptor.getValue();

        assertThat(persistedNotification.getCreatedAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(notificationId);
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(response.getTitle()).isEqualTo("Cập nhật order");
        assertThat(response.getType()).isEqualTo(NotificationType.order);
        assertThat(response.getIsRead()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(
                persistedNotification.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    @Test
    void markAsReadRejectsNotificationOutsideCurrentUserOwnership() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RECORD_NOT_EXISTS));
    }

    @Test
    void getUnreadCountReturnsRepositoryValueForExistingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(4L);

        long unreadCount = notificationService.getUnreadCount(userId);

        assertThat(unreadCount).isEqualTo(4L);
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Mai")
                .lastName("Buyer")
                .role(AppRole.buyer)
                .build();
    }
}
