package com.backend.old_bicycle_project.entity.enums;

public enum PaymentStatus {
    pending("pending"),
    processing("processing"),
    success("success"),
    failed("failed"),
    expired("expired"),
    refunded("refunded");

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
