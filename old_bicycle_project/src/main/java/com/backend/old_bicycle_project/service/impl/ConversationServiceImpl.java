package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.Message;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.MessageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private static final String INSERT_CONVERSATION_IF_ABSENT_SQL = """
            INSERT INTO conversations (product_id, buyer_id, seller_id, created_at, updated_at)
            VALUES (:productId, :buyerId, :sellerId, now(), now())
            ON CONFLICT ON CONSTRAINT uq_conversations_product_buyer_seller DO NOTHING
            RETURNING id
            """;

    private final ConversationRepository conversationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    @Transactional
    public ConversationResponseDTO createOrGetConversation(UUID productId, UUID buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User seller = product.getSeller();

        if (buyer.getId().equals(seller.getId())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Optional<Conversation> existingConversation = findExistingConversation(productId, buyerId, seller.getId());
        if (existingConversation.isPresent()) {
            return mapToDTO(existingConversation.get(), buyerId);
        }

        UUID createdConversationId = tryInsertConversation(productId, buyerId, seller.getId());

        if (createdConversationId == null) {
            Conversation conversation = findExistingConversation(productId, buyerId, seller.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
            return mapToDTO(conversation, buyerId);
        }

        Conversation conversation = conversationRepository.findById(createdConversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        publishNewConversationNotification(conversation, product, buyer, seller);
        return mapToDTO(conversation, buyerId);
    }

    @Override
    public List<ConversationResponseDTO> getUserConversations(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);
        return conversations.stream()
                .map(conversation -> mapToDTO(conversation, userId))
                .collect(Collectors.toList());
    }

    @Override
    public ConversationResponseDTO getConversationById(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        return mapToDTO(conversation, null);
    }

    private Optional<Conversation> findExistingConversation(UUID productId, UUID buyerId, UUID sellerId) {
        return conversationRepository.findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(
                productId,
                buyerId,
                sellerId
        );
    }

    private UUID tryInsertConversation(UUID productId, UUID buyerId, UUID sellerId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productId", productId)
                .addValue("buyerId", buyerId)
                .addValue("sellerId", sellerId);

        try {
            return namedParameterJdbcTemplate.queryForObject(
                    INSERT_CONVERSATION_IF_ABSENT_SQL,
                    params,
                    UUID.class
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void publishNewConversationNotification(
            Conversation conversation,
            Product product,
            User buyer,
            User seller
    ) {
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                seller.getId(),
                "Có cuộc trò chuyện mới",
                buyer.getFullName() + " vừa bắt đầu cuộc trò chuyện mới về sản phẩm " + product.getTitle() + ".",
                NotificationType.chat,
                buildConversationNotificationMetadata(conversation.getId(), product.getId())
        ));
    }

    private String buildConversationNotificationMetadata(UUID conversationId, UUID productId) {
        return "{\"conversationId\":\"" + conversationId + "\",\"productId\":\"" + productId + "\"}";
    }

    private ConversationResponseDTO mapToDTO(Conversation conversation, UUID currentUserId) {
        Optional<Message> latestMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());
        long unreadCount = currentUserId == null
                ? 0
                : messageRepository.countUnreadMessagesForUser(conversation.getId(), currentUserId);

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .productId(conversation.getProduct().getId())
                .productTitle(conversation.getProduct().getTitle())
                .buyerId(conversation.getBuyer().getId())
                .buyerName(conversation.getBuyer().getFullName())
                .sellerId(conversation.getSeller().getId())
                .sellerName(conversation.getSeller().getFullName())
                .lastMessage(latestMessage.map(Message::getContent).orElse(""))
                .unreadCount(unreadCount)
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
