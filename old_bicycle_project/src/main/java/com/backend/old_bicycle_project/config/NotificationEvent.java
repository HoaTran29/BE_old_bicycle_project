package com.backend.old_bicycle_project.config;

import com.backend.old_bicycle_project.entity.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final UUID userId;
    private final String title;
    private final String content;
    private final NotificationType type;
    private final String metadata;

    public NotificationEvent(Object source, UUID userId, String title, String content, NotificationType type, String metadata) {
        super(source);
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.metadata = metadata;
    }
}
