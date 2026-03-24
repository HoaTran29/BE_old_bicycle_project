package com.backend.old_bicycle_project.entity.enums;

public enum OrderFundingStatus {
    unpaid,
    awaiting_payment,
    held,
    seller_payout_pending,
    released,
    refund_pending,
    refund_pending_transfer,
    refunded
}
