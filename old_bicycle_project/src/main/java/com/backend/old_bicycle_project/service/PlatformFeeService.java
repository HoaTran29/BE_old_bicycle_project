package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;

import java.math.BigDecimal;

public interface PlatformFeeService {

    PlatformFeeQuote calculate(BigDecimal totalAmount, BigDecimal protectedAmount, PaymentMethod paymentMethod);

    record PlatformFeeQuote(
            BigDecimal feeBaseAmount,
            BigDecimal platformFeeRate,
            BigDecimal platformFeeTotal,
            BigDecimal buyerFeeAmount,
            BigDecimal sellerFeeAmount,
            BigDecimal buyerChargeAmount,
            BigDecimal sellerGrossPayoutAmount,
            BigDecimal sellerNetPayoutAmount,
            PlatformFeeStatus platformFeeStatus
    ) {
    }
}
