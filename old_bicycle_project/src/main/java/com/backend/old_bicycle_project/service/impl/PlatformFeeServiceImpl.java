package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.service.PlatformFeeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PlatformFeeServiceImpl implements PlatformFeeService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.0200");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MIN_PLATFORM_FEE = new BigDecimal("1000");
    private static final BigDecimal MAX_PLATFORM_FEE = new BigDecimal("500000");
    private static final BigDecimal TWO = new BigDecimal("2");

    @Override
    public PlatformFeeQuote calculate(BigDecimal totalAmount, BigDecimal protectedAmount, PaymentMethod paymentMethod) {
        BigDecimal normalizedTotalAmount = normalize(totalAmount);
        BigDecimal normalizedProtectedAmount = normalize(protectedAmount);

        if (!isPlatformFeeApplicable(paymentMethod)
                || normalizedTotalAmount.compareTo(BigDecimal.ZERO) <= 0
                || normalizedProtectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PlatformFeeQuote(
                    normalizedTotalAmount,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    normalizedProtectedAmount,
                    normalizedProtectedAmount,
                    normalizedProtectedAmount,
                    PlatformFeeStatus.not_applicable);
        }

        BigDecimal rawPlatformFee = normalizedTotalAmount.multiply(PLATFORM_FEE_RATE);
        BigDecimal roundedPlatformFee = rawPlatformFee
                .divide(ONE_THOUSAND, 0, RoundingMode.HALF_UP)
                .multiply(ONE_THOUSAND);
        BigDecimal platformFeeTotal = clamp(roundedPlatformFee, MIN_PLATFORM_FEE, MAX_PLATFORM_FEE);
        BigDecimal buyerFeeAmount = platformFeeTotal.divide(TWO, 0, RoundingMode.HALF_UP);
        BigDecimal sellerFeeAmount = platformFeeTotal.subtract(buyerFeeAmount);
        BigDecimal buyerChargeAmount = normalizedProtectedAmount.add(buyerFeeAmount);
        BigDecimal sellerGrossPayoutAmount = normalizedProtectedAmount;
        BigDecimal sellerNetPayoutAmount = normalizedProtectedAmount.subtract(sellerFeeAmount);

        return new PlatformFeeQuote(
                normalizedTotalAmount,
                PLATFORM_FEE_RATE,
                platformFeeTotal,
                buyerFeeAmount,
                sellerFeeAmount,
                buyerChargeAmount,
                sellerGrossPayoutAmount,
                sellerNetPayoutAmount,
                PlatformFeeStatus.pending);
    }

    private boolean isPlatformFeeApplicable(PaymentMethod paymentMethod) {
        return paymentMethod != null && paymentMethod != PaymentMethod.cash;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
