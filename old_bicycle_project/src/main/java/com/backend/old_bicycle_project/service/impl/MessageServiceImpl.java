package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.MessageRequestDTO;
import com.backend.old_bicycle_project.dto.response.MessageResponseDTO;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.Message;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.MessageRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(MessageRequestDTO requestDTO) {
        Conversation conversation = conversationRepository.findById(requestDTO.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        User sender = userRepository.findById(requestDTO.getSenderId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateParticipant(conversation, sender.getId());

        // Create message
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(requestDTO.getContent())
                .imageUrl(requestDTO.getImageUrl())
                .isRead(false)
                .build();
                
        message = messageRepository.save(message);
        
        // Update conversation's updatedAt timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Determine recipient
        UUID recipientId = sender.getId().equals(conversation.getBuyer().getId()) 
                ? conversation.getSeller().getId() 
                : conversation.getBuyer().getId();

        // Publish Notification Event to the recipient
        eventPublisher.publishEvent(new com.backend.old_bicycle_project.config.NotificationEvent(
                this,
                recipientId,
                "Tin nhắn mới",
                sender.getFullName() + " đã gửi cho bạn một tin nhắn: " + (message.getContent().length() > 20 ? message.getContent().substring(0, 20) + "..." : message.getContent()),
                com.backend.old_bicycle_project.entity.enums.NotificationType.chat,
                "{\"conversationId\": \"" + conversation.getId() + "\"}"
        ));

        return mapToDTO(message);
    }

    @Override
    public Page<MessageResponseDTO> getMessagesByConversation(UUID conversationId, UUID userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        validateParticipant(conversation, userId);

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        validateParticipant(conversation, userId);
        messageRepository.markMessagesAsRead(conversationId, userId);
    }

    private void validateParticipant(Conversation conversation, UUID userId) {
        boolean isParticipant = conversation.getBuyer().getId().equals(userId)
                || conversation.getSeller().getId().equals(userId);
        if (!isParticipant) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private MessageResponseDTO mapToDTO(Message message) {
        return MessageResponseDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getFullName() : "Unknown")
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
