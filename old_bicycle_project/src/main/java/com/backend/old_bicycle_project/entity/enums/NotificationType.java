package com.backend.old_bicycle_project.entity.enums;

public enum NotificationType {
    ORDER("order"),
    CHAT("chat"),
    SYSTEM("system"),
    INSPECTION("inspection"),
    PROMOTION("promotion"),
    WISHLIST("wishlist");

    private final String type;

    NotificationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
