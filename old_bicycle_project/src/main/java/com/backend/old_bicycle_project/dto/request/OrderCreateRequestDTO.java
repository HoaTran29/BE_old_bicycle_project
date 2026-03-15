package com.backend.old_bicycle_project.dto.request;

import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequestDTO {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @DecimalMin(value = "0.01", message = "Upfront amount must be greater than 0")
    private BigDecimal upfrontAmount;

    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")
    private BigDecimal depositAmount;

    @DecimalMin(value = "0.00", message = "Service fee cannot be negative")
    private BigDecimal serviceFee;

    @Builder.Default
    private PaymentOption paymentOption = PaymentOption.partial;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
