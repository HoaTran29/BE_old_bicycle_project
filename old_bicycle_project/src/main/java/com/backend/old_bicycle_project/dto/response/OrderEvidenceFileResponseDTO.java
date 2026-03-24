package com.backend.old_bicycle_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvidenceFileResponseDTO {
    private UUID id;
    private String fileUrl;
    private String fileName;
    private String contentType;
    private Integer sortOrder;
}
