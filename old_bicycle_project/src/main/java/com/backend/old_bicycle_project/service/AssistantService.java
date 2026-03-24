package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.assistant.AssistantChatRequestDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatResponseDTO;
import com.backend.old_bicycle_project.entity.User;

public interface AssistantService {
    AssistantChatResponseDTO chat(AssistantChatRequestDTO request, User currentUser);
}
