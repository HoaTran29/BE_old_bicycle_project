package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderCancelReason;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;
import com.backend.old_bicycle_project.service.OrderService;
import com.backend.old_bicycle_project.service.PayoutService;
import com.backend.old_bicycle_project.service.PlatformFeeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PayoutService payoutService;
    private final OrderEvidenceService orderEvidenceService;
    private final PlatformFeeService platformFeeService;
    private final OrderTransitionSupport orderTransitionSupport;
    private final OrderViewSupport orderViewSupport;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ReviewRepository reviewRepository,
            ApplicationEventPublisher eventPublisher,
            PayoutService payoutService,
            OrderEvidenceService orderEvidenceService,
            PlatformFeeService platformFeeService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.payoutService = payoutService;
        this.orderEvidenceService = orderEvidenceService;
        this.platformFeeService = platformFeeService;
        this.orderTransitionSupport = new OrderTransitionSupport(orderRepository, productRepository, eventPublisher);
        this.orderViewSupport = new OrderViewSupport(reviewRepository, orderEvidenceService);
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(User currentUser, OrderCreateRequestDTO requestDTO) {
        Product product = orderTransitionSupport.lockProduct(requestDTO.getProductId());

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (product.getStatus() != ProductStatus.active && product.getStatus() != ProductStatus.inspected_passed) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (orderRepository.existsExclusiveOrderLockByProductId(product.getId())) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        PaymentOption paymentOption = requestDTO.getPaymentOption() != null
                ? requestDTO.getPaymentOption()
                : PaymentOption.partial;
        BigDecimal requiredUpfrontAmount = orderTransitionSupport.resolveRequiredUpfrontAmount(
                requestDTO,
                product.getPrice(),
                paymentOption
        );
        PlatformFeeService.PlatformFeeQuote platformFeeQuote = platformFeeService.calculate(
                product.getPrice(),
                requiredUpfrontAmount,
                requestDTO.getPaymentMethod()
        );

        if (paymentOption == PaymentOption.partial
                && requestDTO.getPaymentMethod() != PaymentMethod.cash
                && platformFeeQuote.sellerFeeAmount().compareTo(requiredUpfrontAmount) > 0) {
            throw new AppException(ErrorCode.UPFRONT_AMOUNT_TOO_LOW);
        }

        Order order = orderRepository.save(Order.builder()
                .buyer(currentUser)
                .seller(product.getSeller())
                .product(product)
                .totalAmount(product.getPrice())
                .depositAmount(requiredUpfrontAmount)
                .requiredUpfrontAmount(requiredUpfrontAmount)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(product.getPrice())
                .serviceFee(platformFeeQuote.platformFeeTotal())
                .feeBaseAmount(platformFeeQuote.feeBaseAmount())
                .platformFeeRate(platformFeeQuote.platformFeeRate())
                .platformFeeTotal(platformFeeQuote.platformFeeTotal())
                .buyerFeeAmount(platformFeeQuote.buyerFeeAmount())
                .sellerFeeAmount(platformFeeQuote.sellerFeeAmount())
                .buyerChargeAmount(platformFeeQuote.buyerChargeAmount())
                .sellerGrossPayoutAmount(platformFeeQuote.sellerGrossPayoutAmount())
                .sellerNetPayoutAmount(platformFeeQuote.sellerNetPayoutAmount())
                .platformFeeStatus(platformFeeQuote.platformFeeStatus())
                .paymentOption(paymentOption)
                .paymentMethod(requestDTO.getPaymentMethod())
                .fundingStatus(OrderFundingStatus.unpaid)
                .status(OrderStatus.pending)
                .build());

        return orderViewSupport.mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(User currentUser) {
        List<Order> orders = currentUser.getRole() == AppRole.admin
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(currentUser.getId(), currentUser.getId());
        return orderViewSupport.mapOrders(orders);
    }

    @Override
    @Transactional
    public OrderResponseDTO acceptOrder(UUID orderId, User currentUser) {
        Order order = orderTransitionSupport.getOrder(orderId);
        orderTransitionSupport.validateSellerOrAdmin(order, currentUser);
        orderTransitionSupport.lockProduct(order.getProduct().getId());

        if (order.getStatus() != OrderStatus.pending || order.getFundingStatus() != OrderFundingStatus.unpaid) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        if (orderRepository.existsExclusiveOrderLockByProductId(order.getProduct().getId())) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        if (!payoutService.hasCompleteProfile(order.getSeller())) {
            throw new AppException(ErrorCode.PAYOUT_PROFILE_REQUIRED);
        }

        LocalDateTime acceptedAt = LocalDateTime.now();
        order.setAcceptedAt(acceptedAt);
        order.setPaymentDeadline(acceptedAt.plusHours(24));
        order.setFundingStatus(OrderFundingStatus.awaiting_payment);
        order.setCancelReason(null);
        order.setCancelledAt(null);
        order = orderRepository.save(order);
        orderTransitionSupport.rejectCompetingPendingOffers(order, acceptedAt);

        orderTransitionSupport.publishOrderNotification(
                order.getBuyer().getId(),
                "Yêu cầu đặt cọc đã được chấp nhận",
                "Người bán đã chấp nhận đơn hàng và bạn có thể thanh toán tiền ứng trước.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );

        return orderViewSupport.mapToDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmDeposit(UUID orderId, User currentUser) {
        Order order = orderTransitionSupport.getOrder(orderId);
        orderTransitionSupport.validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (order.getPaymentMethod() != PaymentMethod.cash) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
        }
        if (order.getFundingStatus() != OrderFundingStatus.awaiting_payment
                || order.getAcceptedAt() == null
                || order.getPaymentDeadline() == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (order.getPaymentDeadline() != null && order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            orderTransitionSupport.expirePendingPaymentOrder(order, LocalDateTime.now());
            throw new AppException(ErrorCode.PAYMENT_EXPIRED);
        }

        order.setStatus(OrderStatus.deposited);
        order.setPaidAmount(order.getRequiredUpfrontAmount());
        order.setRemainingAmount(order.getTotalAmount().subtract(order.getRequiredUpfrontAmount()));
        order.setFundingStatus(OrderFundingStatus.held);
        return orderViewSupport.mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO completeOrder(UUID orderId, User currentUser, String note, List<MultipartFile> files) {
        Order order = orderTransitionSupport.getOrder(orderId);
        orderTransitionSupport.validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.deposited) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.awaiting_buyer_confirmation);
        order = orderRepository.save(order);
        OrderEvidenceSubmissionResponseDTO sellerEvidence =
                orderEvidenceService.createSellerHandoverEvidence(order, currentUser, note, files);

        orderTransitionSupport.publishOrderNotification(
                order.getBuyer().getId(),
                "Người bán đã báo giao xe",
                "Hãy xác nhận bạn đã nhận xe để hệ thống chuyển sang bước giải ngân cho người bán.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );

        return orderViewSupport.mapToDTO(
                order,
                Map.of(OrderEvidenceType.seller_handover, sellerEvidence)
        );
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmReceived(UUID orderId, User currentUser, String note, List<MultipartFile> files) {
        Order order = orderTransitionSupport.getOrder(orderId);
        orderTransitionSupport.validateBuyerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.awaiting_buyer_confirmation
                || order.getFundingStatus() != OrderFundingStatus.held) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.completed);
        order.setFundingStatus(OrderFundingStatus.seller_payout_pending);
        order.getProduct().setStatus(ProductStatus.sold);
        productRepository.save(order.getProduct());
        order = orderRepository.save(order);

        Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> evidenceByType =
                new java.util.EnumMap<>(OrderEvidenceType.class);
        evidenceByType.putAll(orderEvidenceService.getEvidenceByOrderId(order.getId()));

        Payout payout = payoutService.ensureSellerReleasePayout(order);
        OrderEvidenceSubmissionResponseDTO buyerEvidence =
                orderEvidenceService.createBuyerReceiptEvidence(order, currentUser, note, files);
        if (buyerEvidence != null) {
            evidenceByType.put(OrderEvidenceType.buyer_receipt, buyerEvidence);
        }

        orderTransitionSupport.publishOrderNotification(
                order.getSeller().getId(),
                "Người mua đã xác nhận nhận xe",
                payout.getStatus() == PayoutStatus.profile_required
                        ? "Giao dịch đã hoàn tất. Hãy cập nhật payout profile để nhận khoản cọc."
                        : "Giao dịch đã hoàn tất. Khoản cọc đang chờ admin chuyển khoản thủ công cho bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );

        return orderViewSupport.mapToDTO(order, evidenceByType);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId, User currentUser) {
        Order order = orderTransitionSupport.getOrder(orderId);
        boolean wasSellerReviewRequest = order.getStatus() == OrderStatus.pending
                && order.getFundingStatus() == OrderFundingStatus.unpaid
                && order.getAcceptedAt() == null;

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
        if (orderTransitionSupport.isUnpaidOrAwaitingPayment(order)
                && order.getPlatformFeeStatus() == PlatformFeeStatus.pending) {
            order.setPlatformFeeStatus(PlatformFeeStatus.not_applicable);
            order.setPlatformFeeRecognizedAt(null);
            order.setPlatformFeeReversedAt(null);
        }
        order.setCancelledAt(LocalDateTime.now());
        if (currentUser.getRole() == AppRole.admin) {
            order.setCancelReason(OrderCancelReason.admin_cancelled);
        } else if (order.getSeller().getId().equals(currentUser.getId())) {
            order.setCancelReason(
                    wasSellerReviewRequest ? OrderCancelReason.seller_rejected : OrderCancelReason.seller_cancelled
            );
        } else {
            order.setCancelReason(OrderCancelReason.buyer_cancelled);
        }
        Order savedOrder = orderRepository.save(order);
        orderTransitionSupport.publishCancellationNotifications(savedOrder, currentUser);
        return orderViewSupport.mapToDTO(savedOrder);
    }
}
