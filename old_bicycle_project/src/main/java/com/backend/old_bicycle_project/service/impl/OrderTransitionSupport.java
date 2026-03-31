package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderCancelReason;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

final class OrderTransitionSupport {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    OrderTransitionSupport(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    BigDecimal resolveRequiredUpfrontAmount(
            OrderCreateRequestDTO requestDTO,
            BigDecimal totalAmount,
            PaymentOption paymentOption
    ) {
        if (paymentOption == PaymentOption.full) {
            return totalAmount;
        }

        BigDecimal requestedAmount = requestDTO.getUpfrontAmount() != null
                ? requestDTO.getUpfrontAmount()
                : requestDTO.getDepositAmount();
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        if (requestedAmount.compareTo(totalAmount) > 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return requestedAmount;
    }

    Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
    }

    Product lockProduct(UUID productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    void validateSellerOrAdmin(Order order, User currentUser) {
        boolean isSellerOrAdmin = currentUser.getRole() == AppRole.admin
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isSellerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    void validateBuyerOrAdmin(Order order, User currentUser) {
        boolean isBuyerOrAdmin = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId());
        if (!isBuyerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    void expirePendingPaymentOrder(Order order, LocalDateTime expiredAt) {
        order.setStatus(OrderStatus.cancelled);
        order.setFundingStatus(OrderFundingStatus.unpaid);
        order.setCancelReason(OrderCancelReason.payment_expired);
        order.setCancelledAt(expiredAt);
        orderRepository.save(order);

        String metadata = "{\"orderId\":\"" + order.getId() + "\"}";
        publishOrderNotification(
                order.getBuyer().getId(),
                "Đơn hàng đã hết hạn thanh toán",
                "Bạn chưa hoàn tất thanh toán đúng hạn nên hệ thống đã tự hủy đơn hàng này.",
                metadata
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Đơn hàng tự hủy vì quá hạn thanh toán",
                "Người mua chưa thanh toán đúng hạn nên hệ thống đã tự hủy đơn hàng này.",
                metadata
        );
    }

    void publishCancellationNotifications(Order order, User actor) {
        String metadata = "{\"orderId\":\"" + order.getId() + "\"}";

        if (actor.getRole() == AppRole.admin) {
            publishOrderNotification(
                    order.getBuyer().getId(),
                    "Đơn hàng đã bị admin hủy",
                    "Admin đã hủy đơn hàng này trước khi giao dịch hoàn tất.",
                    metadata
            );
            publishOrderNotification(
                    order.getSeller().getId(),
                    "Đơn hàng đã bị admin hủy",
                    "Admin đã hủy đơn hàng này trước khi giao dịch hoàn tất.",
                    metadata
            );
            return;
        }

        if (order.getSeller().getId().equals(actor.getId())) {
            publishOrderNotification(
                    order.getBuyer().getId(),
                    order.getCancelReason() == OrderCancelReason.seller_rejected
                            ? "Yêu cầu mua đã bị từ chối"
                            : "Đơn hàng đã bị người bán hủy",
                    order.getCancelReason() == OrderCancelReason.seller_rejected
                            ? "Người bán đã từ chối yêu cầu mua này."
                            : "Người bán đã hủy đơn hàng này trước khi giao dịch hoàn tất.",
                    metadata
            );
            return;
        }

        publishOrderNotification(
                order.getSeller().getId(),
                "Người mua đã hủy đơn hàng",
                "Người mua đã hủy đơn hàng này trước khi giao dịch hoàn tất.",
                metadata
        );
    }

    void rejectCompetingPendingOffers(Order acceptedOrder, LocalDateTime decisionAt) {
        List<Order> competingOrders = orderRepository.findByProductIdAndStatusAndFundingStatusOrderByCreatedAtAsc(
                acceptedOrder.getProduct().getId(),
                OrderStatus.pending,
                OrderFundingStatus.unpaid
        );

        List<Order> rejectedOrders = competingOrders.stream()
                .filter(order -> !order.getId().equals(acceptedOrder.getId()))
                .peek(order -> {
                    order.setStatus(OrderStatus.cancelled);
                    order.setCancelReason(OrderCancelReason.seller_rejected);
                    order.setCancelledAt(decisionAt);
                })
                .toList();

        if (rejectedOrders.isEmpty()) {
            return;
        }

        orderRepository.saveAll(rejectedOrders);
        rejectedOrders.forEach(order -> publishOrderNotification(
                order.getBuyer().getId(),
                "Yêu cầu mua không được chọn",
                "Người bán đã chọn một yêu cầu mua khác cho xe này. Đơn của bạn đã được chuyển sang trạng thái từ chối.",
                "{\"orderId\":\"" + order.getId() + "\",\"productId\":\"" + order.getProduct().getId() + "\"}"
        ));
    }

    boolean isUnpaidOrAwaitingPayment(Order order) {
        return order.getFundingStatus() == OrderFundingStatus.unpaid
                || order.getFundingStatus() == OrderFundingStatus.awaiting_payment;
    }

    void publishOrderNotification(UUID userId, String title, String content, String metadata) {
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                userId,
                title,
                content,
                NotificationType.order,
                metadata
        ));
    }
}
