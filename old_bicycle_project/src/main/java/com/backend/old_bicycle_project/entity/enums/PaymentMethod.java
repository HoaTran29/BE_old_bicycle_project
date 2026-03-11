package com.backend.old_bicycle_project.entity.enums;

public enum PaymentMethod {
    TRANSFER("transfer"),
    CASH("cash"),
    ONLINE("online");

    private final String method;

    PaymentMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
