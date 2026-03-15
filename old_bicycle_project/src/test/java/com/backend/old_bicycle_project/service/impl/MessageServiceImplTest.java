package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.MessageRequestDTO;
import com.backend.old_bicycle_project.entity.Message;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.MessageRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void markMessagesAsReadCallsRepositoryForConversationParticipant() {
        UUID conversationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Conversation conversation = conversation(buyerId, sellerId);
        conversation.setId(conversationId);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        messageService.markMessagesAsRead(conversationId, buyerId);

        verify(messageRepository).markMessagesAsRead(conversationId, buyerId);
    }

    @Test
    void getMessagesByConversationRejectsUserOutsideConversation() {
        UUID conversationId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Conversation conversation = conversation(UUID.randomUUID(), UUID.randomUUID());
        conversation.setId(conversationId);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> messageService.getMessagesByConversation(conversationId, outsiderId, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void sendMessageRejectsSenderOutsideConversation() {
        UUID conversationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Conversation conversation = conversation(buyerId, sellerId);
        conversation.setId(conversationId);
        User outsider = user(outsiderId, "outsider@test.dev");

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(outsiderId)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> messageService.sendMessage(MessageRequestDTO.builder()
                        .conversationId(conversationId)
                        .content("Xin chào")
                        .build(), outsiderId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void sendMessageSavesMessageUpdatesConversationAndPublishesNotification() {
        UUID conversationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Conversation conversation = conversation(buyerId, sellerId);
        conversation.setId(conversationId);
        User buyer = user(buyerId, "buyer@test.dev");

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = messageService.sendMessage(MessageRequestDTO.builder()
                .conversationId(conversationId)
                .content("Xe con khong ban?")
                .build(), buyerId);

        assertThat(response.getConversationId()).isEqualTo(conversationId);
        assertThat(response.getSenderId()).isEqualTo(buyerId);
        assertThat(response.getContent()).isEqualTo("Xe con khong ban?");
        verify(messageRepository).save(any(Message.class));
        verify(conversationRepository).save(conversation);
        verify(eventPublisher).publishEvent(any());
    }

    private Conversation conversation(UUID buyerId, UUID sellerId) {
        return Conversation.builder()
                .buyer(user(buyerId, "buyer@test.dev"))
                .seller(user(sellerId, "seller@test.dev"))
                .build();
    }

    private User user(UUID id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .firstName("Test")
                .lastName("User")
                .role(AppRole.buyer)
                .build();
    }
}
