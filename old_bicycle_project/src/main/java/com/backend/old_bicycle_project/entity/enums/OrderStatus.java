package com.backend.old_bicycle_project.entity.enums;

public enum OrderStatus {
    pending("pending"),
    deposited("deposited"),
    awaiting_buyer_confirmation("awaiting_buyer_confirmation"),
    completed("completed"),
    cancelled("cancelled");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
