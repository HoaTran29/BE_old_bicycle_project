package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.MessageRequestDTO;
import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;
import com.backend.old_bicycle_project.dto.response.MessageResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.service.ConversationService;
import com.backend.old_bicycle_project.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatController chatController;

    @Test
    void sendMessageBroadcastsToConversationTopicAndRecipientQueue() {
        UUID conversationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID senderId = buyerId;

        MessageRequestDTO request = MessageRequestDTO.builder()
                .conversationId(conversationId)
                .content("Xe này còn không?")
                .build();
        MessageResponseDTO savedMessage = MessageResponseDTO.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(senderId)
                .senderName("Ngọc Buyer")
                .content("Xe này còn không?")
                .isRead(false)
                .build();
        ConversationResponseDTO conversation = ConversationResponseDTO.builder()
                .id(conversationId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();

        when(messageService.sendMessage(request, senderId)).thenReturn(savedMessage);
        when(conversationService.getConversationById(conversationId)).thenReturn(conversation);

        chatController.sendMessage(request, uuidPrincipal(senderId));

        verify(messagingTemplate).convertAndSend("/topic/conversation/" + conversationId, savedMessage);
        verify(messagingTemplate).convertAndSendToUser(sellerId.toString(), "/queue/messages", savedMessage);
    }

    @Test
    void sendMessageRoutesSellerMessagesToBuyerQueue() {
        UUID conversationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        MessageRequestDTO request = MessageRequestDTO.builder()
                .conversationId(conversationId)
                .content("Minh vua cap nhat gia")
                .build();
        MessageResponseDTO savedMessage = MessageResponseDTO.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(sellerId)
                .senderName("Seller")
                .content("Minh vua cap nhat gia")
                .isRead(false)
                .build();
        ConversationResponseDTO conversation = ConversationResponseDTO.builder()
                .id(conversationId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();

        when(messageService.sendMessage(request, sellerId)).thenReturn(savedMessage);
        when(conversationService.getConversationById(conversationId)).thenReturn(conversation);

        chatController.sendMessage(request, uuidPrincipal(sellerId));

        verify(messagingTemplate).convertAndSend("/topic/conversation/" + conversationId, savedMessage);
        verify(messagingTemplate).convertAndSendToUser(buyerId.toString(), "/queue/messages", savedMessage);
    }

    @Test
    void sendMessageRejectsMissingPrincipal() {
        MessageRequestDTO request = MessageRequestDTO.builder()
                .conversationId(UUID.randomUUID())
                .content("Test")
                .build();

        assertThatThrownBy(() -> chatController.sendMessage(request, null))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void sendMessageRejectsPrincipalWithInvalidUuid() {
        MessageRequestDTO request = MessageRequestDTO.builder()
                .conversationId(UUID.randomUUID())
                .content("Test")
                .build();

        assertThatThrownBy(() -> chatController.sendMessage(request, () -> "not-a-uuid"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void markAsReadDelegatesToMessageService() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        var response = chatController.markAsRead(conversationId, user(userId));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Messages marked as read");
        verify(messageService).markMessagesAsRead(conversationId, userId);
    }

    private Principal uuidPrincipal(UUID userId) {
        return userId::toString;
    }

    private User user(UUID userId) {
        return User.builder()
                .id(userId)
                .build();
    }
}
