package com.backend.old_bicycle_project.entity;

import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.OrderCancelReason;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "required_upfront_amount")
    private BigDecimal requiredUpfrontAmount;

    @Builder.Default
    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount")
    private BigDecimal remainingAmount;

    @Column(name = "service_fee")
    private BigDecimal serviceFee;

    @Builder.Default
    @Column(name = "fee_base_amount", nullable = false)
    private BigDecimal feeBaseAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "platform_fee_rate", nullable = false)
    private BigDecimal platformFeeRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "platform_fee_total", nullable = false)
    private BigDecimal platformFeeTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "buyer_fee_amount", nullable = false)
    private BigDecimal buyerFeeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "seller_fee_amount", nullable = false)
    private BigDecimal sellerFeeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "buyer_charge_amount", nullable = false)
    private BigDecimal buyerChargeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "seller_gross_payout_amount", nullable = false)
    private BigDecimal sellerGrossPayoutAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "seller_net_payout_amount", nullable = false)
    private BigDecimal sellerNetPayoutAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "platform_fee_status", nullable = false, columnDefinition = "platform_fee_status")
    private PlatformFeeStatus platformFeeStatus = PlatformFeeStatus.not_applicable;

    @Column(name = "platform_fee_recognized_at")
    private LocalDateTime platformFeeRecognizedAt;

    @Column(name = "platform_fee_reversed_at")
    private LocalDateTime platformFeeReversedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option")
    private PaymentOption paymentOption = PaymentOption.partial;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "order_status")
    private OrderStatus status = OrderStatus.pending;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "funding_status")
    private OrderFundingStatus fundingStatus = OrderFundingStatus.unpaid;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "payment_method", columnDefinition = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason")
    private OrderCancelReason cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
