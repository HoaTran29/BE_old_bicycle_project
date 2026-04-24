package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.AssistantGatewayProperties;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatRequestDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatResponseDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantMessageDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.NotificationRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PayoutProfileRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private PayoutProfileRepository payoutProfileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private AssistantGatewayProperties properties;
    private AssistantServiceImpl assistantService;

    @BeforeEach
    void setUp() {
        properties = new AssistantGatewayProperties();
        assistantService = new AssistantServiceImpl(
                properties,
                restTemplate,
                new ObjectMapper(),
                orderRepository,
                productRepository,
                inspectionRepository,
                refundRequestRepository,
                payoutRepository,
                payoutProfileRepository,
                notificationRepository
        );
    }

    @Test
    void chatThrowsWhenAssistantGatewayIsNotConfigured() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");

        assertThatThrownBy(() -> assistantService.chat(chatRequest("Đơn hàng của tôi ra sao?"), buyer))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ASSISTANT_NOT_CONFIGURED);
    }

    @Test
    void chatBuildsSellerContextAndParsesGatewayReply() {
        User seller = user(AppRole.seller, "seller@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .title("Giant TCR Advanced")
                .status(ProductStatus.pending_inspection)
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .product(product)
                .status(OrderStatus.pending)
                .fundingStatus(OrderFundingStatus.awaiting_payment)
                .paymentDeadline(LocalDateTime.of(2026, 3, 24, 18, 0))
                .build();

        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setApiBaseUrl("https://ai-gateway.vercel.sh/v1/chat/completions");
        properties.setModel("openai/gpt-4.1-mini");

        when(notificationRepository.countByUserIdAndIsReadFalse(seller.getId())).thenReturn(2L);
        when(payoutProfileRepository.findByUserId(seller.getId())).thenReturn(Optional.empty());
        when(productRepository.countBySellerId(seller.getId())).thenReturn(4L);
        when(productRepository.findBySellerIdAndDeletedAtIsNull(eq(seller.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(seller.getId(), seller.getId()))
                .thenReturn(List.of(order));
        when(payoutRepository.findByRecipientIdAndStatusInOrderByCreatedAtAsc(seller.getId(), List.of(PayoutStatus.pending_transfer)))
                .thenReturn(List.of());
        when(restTemplate.exchange(
                eq("https://ai-gateway.vercel.sh/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Tin đăng gần đây của bạn đang chờ inspection."
                      }
                    }
                  ]
                }
                """));

        AssistantChatResponseDTO response = assistantService.chat(chatRequest("Tin đăng của tôi chưa public vì sao?"), seller);

        assertThat(response.getReply()).isEqualTo("Tin đăng gần đây của bạn đang chờ inspection.");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://ai-gateway.vercel.sh/v1/chat/completions"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captor.getValue().getBody();
        assertThat(payload.get("model")).isEqualTo("openai/gpt-4.1-mini");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");
        assertThat(messages).anySatisfy(message -> {
            assertThat(message.get("role")).isEqualTo("system");
            assertThat(message.get("content")).contains("Giant TCR Advanced");
            assertThat(message.get("content")).contains("pending_inspection");
        });
        assertThat(messages.get(messages.size() - 1)).containsEntry("role", "user");
        assertThat(messages.get(messages.size() - 1)).containsEntry("content", "Tin đăng của tôi chưa public vì sao?");
    }

    private AssistantChatRequestDTO chatRequest(String content) {
        AssistantMessageDTO message = new AssistantMessageDTO();
        message.setRole("user");
        message.setContent(content);

        AssistantChatRequestDTO request = new AssistantChatRequestDTO();
        request.setMessages(List.of(message));
        return request;
    }

    private User user(AppRole role, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName("User")
                .role(role)
                .build();
    }
}
