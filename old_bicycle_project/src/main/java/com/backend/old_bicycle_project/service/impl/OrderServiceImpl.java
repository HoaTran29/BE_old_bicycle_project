package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.service.OrderService;
import com.backend.old_bicycle_project.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PayoutService payoutService;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(User currentUser, OrderCreateRequestDTO requestDTO) {
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (product.getStatus() != ProductStatus.active && product.getStatus() != ProductStatus.inspected_passed) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (orderRepository.existsByProductIdAndStatusIn(
                product.getId(),
                List.of(
                        OrderStatus.pending,
                        OrderStatus.deposited,
                        OrderStatus.awaiting_buyer_confirmation,
                        OrderStatus.completed
                ))) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        PaymentOption paymentOption = requestDTO.getPaymentOption() != null
                ? requestDTO.getPaymentOption()
                : PaymentOption.partial;
        BigDecimal requiredUpfrontAmount = resolveRequiredUpfrontAmount(requestDTO, product.getPrice(), paymentOption);
        BigDecimal serviceFee = requestDTO.getServiceFee() != null ? requestDTO.getServiceFee() : BigDecimal.ZERO;

        Order order = orderRepository.save(Order.builder()
                .buyer(currentUser)
                .seller(product.getSeller())
                .product(product)
                .totalAmount(product.getPrice())
                .depositAmount(requiredUpfrontAmount)
                .requiredUpfrontAmount(requiredUpfrontAmount)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(product.getPrice())
                .serviceFee(serviceFee)
                .paymentOption(paymentOption)
                .paymentMethod(requestDTO.getPaymentMethod())
                .fundingStatus(OrderFundingStatus.unpaid)
                .status(OrderStatus.pending)
                .build());

        return mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(User currentUser) {
        List<Order> orders = currentUser.getRole() == AppRole.admin
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(currentUser.getId(), currentUser.getId());

        Set<UUID> reviewedOrderIds = orders.isEmpty()
                ? Collections.emptySet()
                : reviewRepository.findReviewedOrderIdsByOrderIds(
                orders.stream().map(Order::getId).toList()
        );

        return orders.stream()
                .map(order -> mapToDTO(order, reviewedOrderIds.contains(order.getId())))
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO acceptOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setAcceptedAt(LocalDateTime.now());
        order.setPaymentDeadline(LocalDateTime.now().plusHours(24));
        order.setFundingStatus(OrderFundingStatus.awaiting_payment);
        order = orderRepository.save(order);

        publishOrderNotification(
                order.getBuyer().getId(),
                "Yêu cầu đặt cọc đã được chấp nhận",
                "Người bán đã chấp nhận đơn hàng và bạn có thể thanh toán tiền ứng trước.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );

        return mapToDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmDeposit(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (order.getPaymentMethod() != PaymentMethod.cash) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
        }

        order.setStatus(OrderStatus.deposited);
        order.setAcceptedAt(order.getAcceptedAt() != null ? order.getAcceptedAt() : LocalDateTime.now());
        order.setPaidAmount(order.getRequiredUpfrontAmount());
        order.setRemainingAmount(order.getTotalAmount().subtract(order.getRequiredUpfrontAmount()));
        order.setFundingStatus(OrderFundingStatus.held);
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO completeOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.deposited) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.awaiting_buyer_confirmation);
        order = orderRepository.save(order);

        publishOrderNotification(
                order.getBuyer().getId(),
                "Người bán đã báo giao xe",
                "Hãy xác nhận bạn đã nhận xe để hệ thống chuyển sang bước giải ngân cho người bán.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );

        return mapToDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmReceived(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateBuyerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.awaiting_buyer_confirmation
                || order.getFundingStatus() != OrderFundingStatus.held) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.completed);
        order.setFundingStatus(OrderFundingStatus.seller_payout_pending);
        order.setPaidAmount(order.getTotalAmount());
        order.setRemainingAmount(BigDecimal.ZERO);
        order.getProduct().setStatus(ProductStatus.sold);
        productRepository.save(order.getProduct());
        order = orderRepository.save(order);

        Payout payout = payoutService.ensureSellerReleasePayout(order);

        publishOrderNotification(
                order.getSeller().getId(),
                "Người mua đã xác nhận nhận xe",
                payout.getStatus() == PayoutStatus.profile_required
                        ? "Giao dịch đã hoàn tất. Hãy cập nhật payout profile để nhận khoản cọc."
                        : "Giao dịch đã hoàn tất. Khoản cọc đang chờ admin chuyển khoản thủ công cho bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );

        return mapToDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);

        boolean canCancel = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId())
                || order.getSeller().getId().equals(currentUser.getId());
        if (!canCancel) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.completed
                || order.getStatus() == OrderStatus.cancelled
                || order.getStatus() == OrderStatus.deposited
                || order.getStatus() == OrderStatus.awaiting_buyer_confirmation
                || order.getFundingStatus() == OrderFundingStatus.held
                || order.getFundingStatus() == OrderFundingStatus.refund_pending
                || order.getFundingStatus() == OrderFundingStatus.refund_pending_transfer
                || order.getFundingStatus() == OrderFundingStatus.seller_payout_pending
                || order.getFundingStatus() == OrderFundingStatus.released
                || order.getFundingStatus() == OrderFundingStatus.refunded) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.cancelled);
        if (order.getFundingStatus() == OrderFundingStatus.awaiting_payment) {
            order.setFundingStatus(OrderFundingStatus.unpaid);
        }
        return mapToDTO(orderRepository.save(order));
    }

    private BigDecimal resolveRequiredUpfrontAmount(
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

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
    }

    private void validateSellerOrAdmin(Order order, User currentUser) {
        boolean isSellerOrAdmin = currentUser.getRole() == AppRole.admin
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isSellerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateBuyerOrAdmin(Order order, User currentUser) {
        boolean isBuyerOrAdmin = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId());
        if (!isBuyerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void publishOrderNotification(UUID userId, String title, String content, String metadata) {
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                userId,
                title,
                content,
                NotificationType.order,
                metadata
        ));
    }

    private OrderResponseDTO mapToDTO(Order order) {
        return mapToDTO(order, reviewRepository.existsByOrderId(order.getId()));
    }

    private OrderResponseDTO mapToDTO(Order order, boolean buyerReviewSubmitted) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productTitle(order.getProduct().getTitle())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getFullName())
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .requiredUpfrontAmount(order.getRequiredUpfrontAmount())
                .paidAmount(order.getPaidAmount())
                .remainingAmount(order.getRemainingAmount())
                .serviceFee(order.getServiceFee())
                .paymentOption(order.getPaymentOption())
                .status(order.getStatus())
                .fundingStatus(order.getFundingStatus())
                .paymentMethod(order.getPaymentMethod())
                .buyerReviewSubmitted(buyerReviewSubmitted)
                .acceptedAt(order.getAcceptedAt())
                .paymentDeadline(order.getPaymentDeadline())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
