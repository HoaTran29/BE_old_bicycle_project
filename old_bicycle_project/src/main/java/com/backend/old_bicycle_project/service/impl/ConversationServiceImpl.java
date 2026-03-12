package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.Message;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.MessageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public ConversationResponseDTO createOrGetConversation(UUID productId, UUID buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User seller = product.getSeller();

        // Cannot create conversation with yourself
        if (buyer.getId().equals(seller.getId())) {
            throw new AppException(ErrorCode.INVALID_KEY); // Or specific CODE for invalid action
        }

        // Check if conversation already exists
        Optional<Conversation> existingConversation = conversationRepository.findByProductIdAndBuyerId(productId, buyerId);
        
        if (existingConversation.isPresent()) {
            return mapToDTO(existingConversation.get());
        }

        // Create new
        Conversation conversation = Conversation.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .build();

        conversation = conversationRepository.save(conversation);
        return mapToDTO(conversation);
    }

    @Override
    public List<ConversationResponseDTO> getUserConversations(UUID userId) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);
        return conversations.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ConversationResponseDTO getConversationById(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        return mapToDTO(conversation);
    }

    private ConversationResponseDTO mapToDTO(Conversation conversation) {
        Optional<Message> latestMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .productId(conversation.getProduct().getId())
                .productTitle(conversation.getProduct().getTitle())
                .buyerId(conversation.getBuyer().getId())
                .buyerName(conversation.getBuyer().getFullName())
                .sellerId(conversation.getSeller().getId())
                .sellerName(conversation.getSeller().getFullName())
                .lastMessage(latestMessage.map(Message::getContent).orElse(""))
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
