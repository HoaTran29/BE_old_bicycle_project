package com.backend.old_bicycle_project.entity.enums;

public enum OrderStatus {
    PENDING("pending"),
    DEPOSITED("deposited"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
