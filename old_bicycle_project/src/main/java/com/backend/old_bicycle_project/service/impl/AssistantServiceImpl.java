package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.AssistantGatewayProperties;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatRequestDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatResponseDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantMessageDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.PayoutProfile;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.NotificationRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PayoutProfileRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.AssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {
    private static final int MAX_MESSAGES = 8;
    private static final int MAX_MESSAGE_LENGTH = 1200;
    private static final int MAX_RECENT_ITEMS = 3;

    private final AssistantGatewayProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InspectionRepository inspectionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutProfileRepository payoutProfileRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public AssistantChatResponseDTO chat(AssistantChatRequestDTO request, User currentUser) {
        validateAssistantAvailability();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(buildSystemPrompt()));
        messages.add(systemMessage(buildPlatformRules()));
        messages.add(systemMessage(buildUserContext(currentUser)));
        messages.addAll(sanitizeConversation(request.getMessages()));

        String reply = requestAssistantReply(messages);
        return AssistantChatResponseDTO.builder()
                .reply(reply)
                .build();
    }

    private void validateAssistantAvailability() {
        if (!properties.isEnabled() || isBlank(properties.getApiKey())) {
            throw new AppException(ErrorCode.ASSISTANT_NOT_CONFIGURED);
        }
    }

    private String requestAssistantReply(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("temperature", properties.getTemperature());
        payload.put("max_tokens", properties.getMaxTokens());
        payload.put("messages", messages);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiBaseUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            return extractAssistantReply(response.getBody());
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.ASSISTANT_REQUEST_FAILED);
        }
    }

    private String extractAssistantReply(String responseBody) {
        if (isBlank(responseBody)) {
            throw new AppException(ErrorCode.ASSISTANT_REQUEST_FAILED);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String reply = extractContent(contentNode).trim();
            if (reply.isEmpty()) {
                throw new AppException(ErrorCode.ASSISTANT_REQUEST_FAILED);
            }
            return reply;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.ASSISTANT_REQUEST_FAILED);
        }
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            contentNode.forEach(item -> {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(textNode.asText());
                }
            });
            return builder.toString();
        }
        return "";
    }

    private List<Map<String, String>> sanitizeConversation(List<AssistantMessageDTO> rawMessages) {
        return rawMessages.stream()
                .filter(Objects::nonNull)
                .skip(Math.max(0, rawMessages.size() - MAX_MESSAGES))
                .map(message -> Map.of(
                        "role", normalizeRole(message.getRole()),
                        "content", truncate(message.getContent().trim(), MAX_MESSAGE_LENGTH)
                ))
                .toList();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return "assistant".equals(normalized) ? "assistant" : "user";
    }

    private Map<String, String> systemMessage(String content) {
        return Map.of("role", "system", "content", content);
    }

    private String buildSystemPrompt() {
        return """
                Bạn là Trợ lý BikeExchange của sàn mua bán xe đạp cũ.
                Hãy trả lời bằng tiếng Việt có dấu, ngắn gọn, rõ ràng và đúng ngữ cảnh của người dùng hiện tại.
                Chỉ dùng dữ liệu đã được cung cấp trong context hệ thống và business rules bên dưới.
                Nếu context không đủ để kết luận, hãy nói rõ là chưa đủ dữ liệu và gợi ý người dùng mở đúng màn hình tương ứng.
                Không được bịa trạng thái đơn hàng, trạng thái tin đăng, kết quả inspection, refund, payout hoặc quyền hệ thống.
                Không được khẳng định bạn đã thực hiện một hành động thay người dùng. Bạn chỉ có quyền giải thích và hướng dẫn.
                Ưu tiên trả lời theo cấu trúc: tình trạng hiện tại -> ý nghĩa -> việc người dùng nên làm tiếp.
                """;
    }

    private String buildPlatformRules() {
        return """
                Business rules hiện tại của BikeExchange:
                - Mọi tin đăng muốn public đều phải qua admin moderation và inspection pass trước khi public.
                - Payment quá hạn sẽ tự hết hiệu lực; hệ thống có thể auto-cancel đơn chờ thanh toán khi quá deadline.
                - Nếu webhook nhận tiền đến muộn sau khi đơn đã hết hạn, hệ thống không tự revive đơn mà chuyển sang nhánh refund thủ công.
                - Refund và seller payout hiện đang theo mô hình thủ công có đối soát, dùng payout profile và bank reference.
                - Seller báo đã giao xe nên kèm ảnh bàn giao; buyer xác nhận đã nhận xe có thể kèm ảnh nhận xe.
                - Review do buyer gửi sau khi đơn hoàn tất; seller có thể reply review.
                """;
    }

    private String buildUserContext(User currentUser) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ngữ cảnh người dùng hiện tại:\n");
        builder.append("- userId: ").append(currentUser.getId()).append('\n');
        builder.append("- họ tên: ").append(currentUser.getFullName()).append('\n');
        builder.append("- email: ").append(currentUser.getEmail()).append('\n');
        builder.append("- role: ").append(currentUser.getRole().name()).append('\n');
        builder.append("- unread notifications: ")
                .append(notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId()))
                .append('\n');

        PayoutProfile payoutProfile = payoutProfileRepository.findByUserId(currentUser.getId()).orElse(null);
        builder.append("- payout profile: ")
                .append(payoutProfile == null ? "chưa cấu hình" : "đã cấu hình")
                .append('\n');

        switch (currentUser.getRole()) {
            case buyer -> builder.append(buildBuyerContext(currentUser.getId()));
            case seller -> builder.append(buildSellerContext(currentUser.getId()));
            case inspector -> builder.append(buildInspectorContext(currentUser.getId()));
            case admin -> builder.append(buildAdminContext());
            default -> builder.append("- Không có context bổ sung theo vai trò.\n");
        }

        return builder.toString();
    }

    private String buildBuyerContext(UUID userId) {
        List<Order> recentOrders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .filter(order -> order.getBuyer() != null && userId.equals(order.getBuyer().getId()))
                .limit(MAX_RECENT_ITEMS)
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("- tổng số đơn mua: ").append(orderRepository.countByBuyerId(userId)).append('\n');
        builder.append("- đơn mua gần đây:\n");
        appendOrderSummaries(builder, recentOrders);
        return builder.toString();
    }

    private String buildSellerContext(UUID userId) {
        List<Product> recentProducts = productRepository.findBySellerIdAndDeletedAtIsNull(
                        userId,
                        PageRequest.of(0, MAX_RECENT_ITEMS)
                )
                .getContent();
        List<Order> recentOrders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .filter(order -> order.getSeller() != null && userId.equals(order.getSeller().getId()))
                .limit(MAX_RECENT_ITEMS)
                .toList();
        List<Payout> pendingPayouts = payoutRepository.findByRecipientIdAndStatusInOrderByCreatedAtAsc(
                userId,
                List.of(PayoutStatus.pending_transfer)
        );

        StringBuilder builder = new StringBuilder();
        builder.append("- tổng số tin đăng: ").append(productRepository.countBySellerId(userId)).append('\n');
        builder.append("- tin đăng gần đây:\n");
        recentProducts.forEach(product -> builder.append("  - ")
                .append(product.getTitle())
                .append(" | status=")
                .append(product.getStatus().name())
                .append('\n'));
        builder.append("- đơn bán gần đây:\n");
        appendOrderSummaries(builder, recentOrders);
        builder.append("- payout pending transfer: ").append(pendingPayouts.size()).append('\n');
        return builder.toString();
    }

    private String buildInspectorContext(UUID userId) {
        StringBuilder builder = new StringBuilder();
        builder.append("- tổng inspection đã xử lý: ").append(inspectionRepository.countByInspectorId(userId)).append('\n');
        builder.append("- inspection pass: ").append(inspectionRepository.countByInspectorIdAndPassedTrue(userId)).append('\n');
        builder.append("- inspection cập nhật trong 30 ngày: ")
                .append(inspectionRepository.countByInspectorIdAndUpdatedAtAfter(userId, LocalDateTime.now().minusDays(30)))
                .append('\n');
        return builder.toString();
    }

    private String buildAdminContext() {
        StringBuilder builder = new StringBuilder();
        builder.append("- pending listings: ").append(productRepository.countByStatus(ProductStatus.pending)).append('\n');
        builder.append("- pending inspection queue: ")
                .append(productRepository.countByStatus(ProductStatus.pending_inspection))
                .append('\n');
        builder.append("- pending disputes: ").append(refundRequestRepository.countByStatus(RefundStatus.pending)).append('\n');
        builder.append("- pending payouts: ").append(payoutRepository.countByStatus(PayoutStatus.pending_transfer)).append('\n');
        builder.append("- total active listings: ").append(productRepository.countByStatus(ProductStatus.active)).append('\n');
        return builder.toString();
    }

    private void appendOrderSummaries(StringBuilder builder, List<Order> orders) {
        if (orders.isEmpty()) {
            builder.append("  - Không có dữ liệu gần đây.\n");
            return;
        }
        orders.forEach(order -> builder.append("  - ")
                .append(order.getProduct() != null ? order.getProduct().getTitle() : "Đơn không có sản phẩm")
                .append(" | status=")
                .append(order.getStatus().name())
                .append(" | funding=")
                .append(order.getFundingStatus().name())
                .append(" | deadline=")
                .append(order.getPaymentDeadline())
                .append('\n'));
    }

    private String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
