package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.MessageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Test
    void createOrGetConversationReturnsExistingConversationWithoutCreatingDuplicate() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        User buyer = user(buyerId, "buyer@test.dev", "Buyer", "Sweep");
        User seller = user(sellerId, "seller@test.dev", "MTB", "Seller");
        Product product = product(productId, "Giant 2026", seller);
        Conversation existingConversation = conversation(conversationId, product, buyer, seller);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(conversationRepository.findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(productId, buyerId, sellerId))
                .thenReturn(Optional.of(existingConversation));
        stubConversationReadModels(existingConversation, buyerId, "Tin nhắn gần nhất", 2);

        ConversationResponseDTO response = conversationService.createOrGetConversation(productId, buyerId);

        assertThat(response.getId()).isEqualTo(conversationId);
        assertThat(response.getUnreadCount()).isEqualTo(2);
        assertThat(response.getLastMessage()).isEqualTo("Tin nhắn gần nhất");
        verify(namedParameterJdbcTemplate, never()).queryForObject(anyString(), any(MapSqlParameterSource.class), eq(UUID.class));
        verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void createOrGetConversationCreatesNewConversationAndPublishesNotification() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        User buyer = user(buyerId, "buyer@test.dev", "Buyer", "Sweep");
        User seller = user(sellerId, "seller@test.dev", "MTB", "Seller");
        Product product = product(productId, "Giant 2026", seller);
        Conversation createdConversation = conversation(conversationId, product, buyer, seller);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(conversationRepository.findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(productId, buyerId, sellerId))
                .thenReturn(Optional.empty());
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(UUID.class)))
                .thenReturn(conversationId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(createdConversation));
        stubConversationReadModels(createdConversation, buyerId, "", 0);

        ConversationResponseDTO response = conversationService.createOrGetConversation(productId, buyerId);

        assertThat(response.getId()).isEqualTo(conversationId);
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getSellerName()).isEqualTo("MTB Seller");

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent notificationEvent = eventCaptor.getValue();
        assertThat(notificationEvent.getUserId()).isEqualTo(sellerId);
        assertThat(notificationEvent.getType()).isEqualTo(NotificationType.chat);
        assertThat(notificationEvent.getTitle()).isEqualTo("Có cuộc trò chuyện mới");
        assertThat(notificationEvent.getContent()).isEqualTo("Buyer Sweep vừa bắt đầu cuộc trò chuyện mới về sản phẩm Giant 2026.");
        assertThat(notificationEvent.getMetadata()).isEqualTo(
                "{\"conversationId\":\"" + conversationId + "\",\"productId\":\"" + productId + "\"}"
        );
    }

    @Test
    void createOrGetConversationReturnsExistingConversationWhenAnotherRequestWinsTheRace() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        User buyer = user(buyerId, "buyer@test.dev", "Buyer", "Sweep");
        User seller = user(sellerId, "seller@test.dev", "MTB", "Seller");
        Product product = product(productId, "Giant 2026", seller);
        Conversation existingConversation = conversation(conversationId, product, buyer, seller);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(conversationRepository.findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(productId, buyerId, sellerId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingConversation));
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(UUID.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        stubConversationReadModels(existingConversation, buyerId, "Đã có từ thread khác", 0);

        ConversationResponseDTO response = conversationService.createOrGetConversation(productId, buyerId);

        assertThat(response.getId()).isEqualTo(conversationId);
        assertThat(response.getLastMessage()).isEqualTo("Đã có từ thread khác");
        verify(conversationRepository, times(2))
                .findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(productId, buyerId, sellerId);
        verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
    }

    private void stubConversationReadModels(
            Conversation conversation,
            UUID currentUserId,
            String latestMessageContent,
            long unreadCount
    ) {
        if (latestMessageContent == null || latestMessageContent.isBlank()) {
            when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()))
                    .thenReturn(Optional.empty());
        } else {
            when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()))
                    .thenReturn(Optional.of(com.backend.old_bicycle_project.entity.Message.builder()
                            .content(latestMessageContent)
                            .build()));
        }
        when(messageRepository.countUnreadMessagesForUser(conversation.getId(), currentUserId))
                .thenReturn(unreadCount);
    }

    private Conversation conversation(UUID conversationId, Product product, User buyer, User seller) {
        return Conversation.builder()
                .id(conversationId)
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .build();
    }

    private Product product(UUID productId, String title, User seller) {
        return Product.builder()
                .id(productId)
                .title(title)
                .seller(seller)
                .build();
    }

    private User user(UUID userId, String email, String firstName, String lastName) {
        return User.builder()
                .id(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(AppRole.buyer)
                .build();
    }
}
