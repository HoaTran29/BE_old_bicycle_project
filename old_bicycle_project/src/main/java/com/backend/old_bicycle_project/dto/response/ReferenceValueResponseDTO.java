package com.backend.old_bicycle_project.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReferenceValueResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
