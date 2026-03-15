package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private NotificationType type;
    private Boolean isRead;
    private String metadata;
    private LocalDateTime createdAt;
}
