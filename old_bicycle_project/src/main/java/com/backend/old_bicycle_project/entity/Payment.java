package com.backend.old_bicycle_project.entity;

import com.backend.old_bicycle_project.entity.enums.PaymentGateway;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "protected_amount", nullable = false)
    private BigDecimal protectedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "buyer_fee_amount", nullable = false)
    private BigDecimal buyerFeeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway")
    private PaymentGateway gateway = PaymentGateway.manual;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "method", nullable = false, columnDefinition = "payment_method")
    private PaymentMethod method;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "phase")
    private PaymentPhase phase = PaymentPhase.upfront;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "payment_status")
    private PaymentStatus status = PaymentStatus.pending;

    @Column(name = "gateway_order_code", unique = true)
    private String gatewayOrderCode;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(name = "gateway_response", columnDefinition = "jsonb")
    @ColumnTransformer(read = "gateway_response::text", write = "?::jsonb")
    private String gatewayResponse;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
