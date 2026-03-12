package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.NotificationResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<NotificationResponseDTO>>> getUserNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDTO> notifications =
                notificationService.getUserNotifications(currentUser.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<NotificationResponseDTO>>builder()
                .code(200)
                .message("Fetched notifications successfully")
                .result(notifications)
                .build());
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .message("Fetched unread count successfully")
                .result(count)
                .build());
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable java.util.UUID notificationId,
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAsRead(notificationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Notification marked as read")
                .build());
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("All notifications marked as read")
                .build());
    }
}
