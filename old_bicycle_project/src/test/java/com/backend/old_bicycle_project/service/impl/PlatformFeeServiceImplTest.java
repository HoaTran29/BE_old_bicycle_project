package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.service.PlatformFeeService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformFeeServiceImplTest {

    private final PlatformFeeService platformFeeService = new PlatformFeeServiceImpl();

    @Test
    void calculateTransferFeeUsesTotalAmountAndSplitAcrossBuyerAndSeller() {
        PlatformFeeService.PlatformFeeQuote quote = platformFeeService.calculate(
                new BigDecimal("20000000"),
                new BigDecimal("4000000"),
                PaymentMethod.transfer
        );

        assertThat(quote.feeBaseAmount()).isEqualByComparingTo("20000000");
        assertThat(quote.platformFeeRate()).isEqualByComparingTo("0.0200");
        assertThat(quote.platformFeeTotal()).isEqualByComparingTo("400000");
        assertThat(quote.buyerFeeAmount()).isEqualByComparingTo("200000");
        assertThat(quote.sellerFeeAmount()).isEqualByComparingTo("200000");
        assertThat(quote.buyerChargeAmount()).isEqualByComparingTo("4200000");
        assertThat(quote.sellerGrossPayoutAmount()).isEqualByComparingTo("4000000");
        assertThat(quote.sellerNetPayoutAmount()).isEqualByComparingTo("3800000");
        assertThat(quote.platformFeeStatus()).isEqualTo(PlatformFeeStatus.pending);
    }

    @Test
    void calculateCashOrderReturnsZeroFeeAndNotApplicableStatus() {
        PlatformFeeService.PlatformFeeQuote quote = platformFeeService.calculate(
                new BigDecimal("20000000"),
                new BigDecimal("4000000"),
                PaymentMethod.cash
        );

        assertThat(quote.platformFeeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(quote.buyerFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(quote.sellerFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(quote.buyerChargeAmount()).isEqualByComparingTo("4000000");
        assertThat(quote.sellerNetPayoutAmount()).isEqualByComparingTo("4000000");
        assertThat(quote.platformFeeStatus()).isEqualTo(PlatformFeeStatus.not_applicable);
    }
}
