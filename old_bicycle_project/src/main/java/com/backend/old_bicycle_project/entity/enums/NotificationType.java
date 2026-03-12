package com.backend.old_bicycle_project.entity.enums;

public enum NotificationType {
    order("order"),
    chat("chat"),
    system("system"),
    inspection("inspection"),
    promotion("promotion"),
    wishlist("wishlist");

    private final String type;

    NotificationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
