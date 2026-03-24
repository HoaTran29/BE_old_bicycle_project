package com.backend.old_bicycle_project.dto.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssistantChatRequestDTO {

    @NotEmpty(message = "INVALID_KEY")
    @Valid
    private List<AssistantMessageDTO> messages;
}
