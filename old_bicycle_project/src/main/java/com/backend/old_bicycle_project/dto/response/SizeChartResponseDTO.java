package com.backend.old_bicycle_project.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SizeChartResponseDTO {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private List<SizeChartRowResponseDTO> rows;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
