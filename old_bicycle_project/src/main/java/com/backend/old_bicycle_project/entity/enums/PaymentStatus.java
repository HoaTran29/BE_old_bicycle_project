package com.backend.old_bicycle_project.entity.enums;

public enum PaymentStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    SUCCESS("success"),
    FAILED("failed"),
    REFUNDED("refunded");

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
