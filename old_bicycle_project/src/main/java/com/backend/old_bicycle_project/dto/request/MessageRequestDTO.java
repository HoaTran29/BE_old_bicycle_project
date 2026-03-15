package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequestDTO {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    private UUID senderId;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String imageUrl;
}
