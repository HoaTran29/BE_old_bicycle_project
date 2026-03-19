package com.backend.old_bicycle_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutProfileResponseDTO {
    private UUID id;
    private UUID userId;
    private String bankCode;
    private String bankBin;
    private String accountNumber;
    private String accountName;
    private LocalDateTime updatedAt;
}
