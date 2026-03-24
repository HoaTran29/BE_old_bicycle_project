package com.backend.old_bicycle_project.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SizeChartRowResponseDTO {

    private UUID id;
    private String frameSize;
    private Integer heightMinCm;
    private Integer heightMaxCm;
    private String note;
    private Integer displayOrder;
}
