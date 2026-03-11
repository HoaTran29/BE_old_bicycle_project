package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.MessageRequestDTO;
import com.backend.old_bicycle_project.dto.response.MessageResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageService {

    /**
     * Save and process a new chat message
     */
    MessageResponseDTO sendMessage(MessageRequestDTO requestDTO);

    /**
     * Get paginated messages for a conversation
     */
    Page<MessageResponseDTO> getMessagesByConversation(UUID conversationId, Pageable pageable);

    /**
     * Mark all unread messages in a conversation as read for a specific user
     */
    void markMessagesAsRead(UUID conversationId, UUID userId);
}
