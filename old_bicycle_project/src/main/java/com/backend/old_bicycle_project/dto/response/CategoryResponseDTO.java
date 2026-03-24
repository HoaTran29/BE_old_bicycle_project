package com.backend.old_bicycle_project.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CategoryResponseDTO {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private String parentName;
    private LocalDateTime createdAt;
}
