package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.MessageRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ConversationResponseDTO;
import com.backend.old_bicycle_project.dto.response.MessageResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.service.ConversationService;
import com.backend.old_bicycle_project.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    // --- REST APIs for pulling data ---

    @GetMapping("/api/conversations/me")
    public ResponseEntity<ApiResponse<List<ConversationResponseDTO>>> getUserConversations(
            @AuthenticationPrincipal User currentUser) {
        List<ConversationResponseDTO> conversations = conversationService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<List<ConversationResponseDTO>>builder()
                .code(200)
                .message("Fetched conversations successfully")
                .result(conversations)
                .build());
    }

    @PostMapping("/api/conversations")
    public ResponseEntity<ApiResponse<ConversationResponseDTO>> createOrGetConversation(
            @RequestParam UUID productId,
            @AuthenticationPrincipal User currentUser) {
        ConversationResponseDTO conversation =
                conversationService.createOrGetConversation(productId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<ConversationResponseDTO>builder()
                .code(200)
                .message("Conversation retrieved/created successfully")
                .result(conversation)
                .build());
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponseDTO>>> getMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponseDTO> messages =
                messageService.getMessagesByConversation(conversationId, currentUser.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<MessageResponseDTO>>builder()
                .code(200)
                .message("Fetched messages successfully")
                .result(messages)
                .build());
    }

    @PutMapping("/api/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User currentUser) {
        messageService.markMessagesAsRead(conversationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Messages marked as read")
                .build());
    }

    // --- WebSocket Endpoints for Real-time push ---

    /**
     * Client sends STOMP message to "/app/chat.sendMessage"
     * Server processes, saves to DB, then broadcasts to:
     * 1. "/topic/conversation/{conversationId}" -> For currently open chat window
     * 2. "/queue/user/{recipientId}" -> For global notification updates
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload @Valid MessageRequestDTO chatMessage, Principal principal) {
        UUID senderId = resolveSenderId(principal);

        // Save to DB
        MessageResponseDTO savedMessage = messageService.sendMessage(chatMessage, senderId);
        
        // Find recipient logic
        ConversationResponseDTO conversation = conversationService.getConversationById(chatMessage.getConversationId());
        UUID recipientId = savedMessage.getSenderId().equals(conversation.getBuyerId())
                ? conversation.getSellerId() 
                : conversation.getBuyerId();

        // 1. Broadcast to the specific conversation channel (for those who have it open)
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + chatMessage.getConversationId(), 
                savedMessage
        );

        // 2. Push to recipient's private queue (for global notification / unread badge)
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                savedMessage
        );
    }

    private UUID resolveSenderId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return UUID.fromString(principal.getName());
    }
}
