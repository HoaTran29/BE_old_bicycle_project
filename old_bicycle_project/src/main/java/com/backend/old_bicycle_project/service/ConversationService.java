package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    /**
     * Create a new conversation or get existing one for a product and buyer
     */
    ConversationResponseDTO createOrGetConversation(UUID productId, UUID buyerId);

    /**
     * Get all conversations for a specific user (can be buyer or seller)
     */
    List<ConversationResponseDTO> getUserConversations(UUID userId);
    
    /**
     * Get conversation details by ID
     */
    ConversationResponseDTO getConversationById(UUID conversationId);
}
