package com.backend.old_bicycle_project.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SepayWebhookRequestDTO {
    private Long id;
    private String gateway;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;
    private String accountNumber;
    private String transferType;
    private BigDecimal transferAmount;
    private String accumulated;
    private String code;
    private String content;
    private String referenceCode;
    private String description;
}
