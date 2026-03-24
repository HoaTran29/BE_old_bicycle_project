package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentService paymentService;

    @Scheduled(
            fixedDelayString = "${payment.expiry.scan-interval-ms:60000}",
            initialDelayString = "${payment.expiry.initial-delay-ms:15000}"
    )
    public void expireOverduePayments() {
        int expiredCount = paymentService.expireOverdueUpfrontPayments();
        if (expiredCount > 0) {
            log.info("Auto-expired {} overdue upfront payment order(s)", expiredCount);
        }
    }
}
