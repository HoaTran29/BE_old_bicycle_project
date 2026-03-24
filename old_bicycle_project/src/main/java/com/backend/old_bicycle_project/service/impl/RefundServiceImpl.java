package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminRefundResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;
import com.backend.old_bicycle_project.service.PayoutService;
import com.backend.old_bicycle_project.service.RefundService;
import com.backend.old_bicycle_project.specification.RefundRequestSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PayoutService payoutService;
    private final OrderEvidenceService orderEvidenceService;

    @Override
    @Transactional
    public RefundResponseDTO requestRefund(UUID orderId, User currentUser, RefundCreateRequestDTO requestDTO) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        boolean refundableStatus = order.getStatus() == OrderStatus.deposited
                || order.getStatus() == OrderStatus.awaiting_buyer_confirmation;
        if (!refundableStatus || order.getFundingStatus() != OrderFundingStatus.held) {
            throw new AppException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        if (refundRequestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, RefundStatus.pending).isPresent()) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Payment payment = paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(orderId, PaymentPhase.upfront)
                .filter(candidate -> candidate.getStatus() == PaymentStatus.success)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_ALLOWED));

        BigDecimal refundAmount = order.getPaidAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.REFUND_NOT_ALLOWED);
        }
        if (requestDTO.getAmount() != null && requestDTO.getAmount().compareTo(refundAmount) != 0) {
            throw new AppException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        RefundRequest refundRequest = refundRequestRepository.save(RefundRequest.builder()
                .order(order)
                .payment(payment)
                .requester(currentUser)
                .amount(refundAmount)
                .reason(requestDTO.getReason())
                .evidenceNote(requestDTO.getEvidenceNote())
                .status(RefundStatus.pending)
                .build());

        order.setFundingStatus(OrderFundingStatus.refund_pending);
        orderRepository.save(order);

        publishOrderNotification(
                currentUser.getId(),
                "Đã tạo yêu cầu hoàn tiền",
                "Yêu cầu hoàn tiền của bạn đã được gửi cho admin xem xét.",
                "{\"orderId\":\"" + order.getId() + "\",\"refundId\":\"" + refundRequest.getId() + "\"}"
        );
        publishAdminRefundPendingNotification(order, refundRequest);

        return mapToDTO(refundRequest);
    }

    @Override
    @Transactional
    public RefundResponseDTO reviewRefund(UUID refundId, User currentUser, RefundReviewRequestDTO requestDTO) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        Order order = refundRequest.getOrder();
        Payment payment = refundRequest.getPayment();

        switch (requestDTO.getStatus()) {
            case approved -> approveRefund(refundRequest, order, currentUser, requestDTO);
            case rejected -> rejectRefund(refundRequest, order, currentUser, requestDTO);
            case completed -> completeRefund(refundRequest, currentUser, requestDTO);
            default -> throw new AppException(ErrorCode.INVALID_STATUS);
        }

        refundRequestRepository.save(refundRequest);
        if (refundRequest.getStatus() != RefundStatus.completed) {
            paymentRepository.save(payment);
            orderRepository.save(order);
        }

        return mapToDTO(refundRequest);
    }

    @Override
    public Page<AdminRefundResponseDTO> getAdminRefunds(String keyword, RefundStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RefundRequest> refundPage = refundRequestRepository.findAll(
                RefundRequestSpecification.fromAdminFilter(keyword, status),
                pageable
        );

        Set<UUID> productIds = refundPage.getContent().stream()
                .map(RefundRequest::getOrder)
                .filter(order -> order != null && order.getProduct() != null)
                .map(order -> order.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> inspectedProductIds = productIds.isEmpty()
                ? Set.of()
                : new HashSet<>(inspectionRepository.findDistinctProductIdsWithInspection(productIds));
        Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> evidenceByOrder =
                orderEvidenceService.getEvidenceByOrderIds(
                        refundPage.getContent().stream()
                                .map(RefundRequest::getOrder)
                                .filter(order -> order != null)
                                .map(Order::getId)
                                .toList()
                );

        return refundPage.map(refundRequest -> mapToAdminDTO(
                refundRequest,
                refundRequest.getOrder() != null
                        && refundRequest.getOrder().getProduct() != null
                        && inspectedProductIds.contains(refundRequest.getOrder().getProduct().getId()),
                evidenceByOrder.getOrDefault(
                        refundRequest.getOrder() != null ? refundRequest.getOrder().getId() : null,
                        Map.of()
                )
        ));
    }

    private void approveRefund(
            RefundRequest refundRequest,
            Order order,
            User currentUser,
            RefundReviewRequestDTO requestDTO
    ) {
        if (refundRequest.getStatus() != RefundStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        refundRequest.setStatus(RefundStatus.approved);
        refundRequest.setAdminNote(requestDTO.getAdminNote());
        refundRequest.setReviewedBy(currentUser);
        refundRequest.setReviewedAt(LocalDateTime.now());
        order.setFundingStatus(OrderFundingStatus.refund_pending_transfer);

        payoutService.ensureRefundPayout(refundRequest);
    }

    private void rejectRefund(
            RefundRequest refundRequest,
            Order order,
            User currentUser,
            RefundReviewRequestDTO requestDTO
    ) {
        if (refundRequest.getStatus() != RefundStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        refundRequest.setStatus(RefundStatus.rejected);
        refundRequest.setAdminNote(requestDTO.getAdminNote());
        refundRequest.setReviewedBy(currentUser);
        refundRequest.setReviewedAt(LocalDateTime.now());
        order.setFundingStatus(OrderFundingStatus.held);

        publishOrderNotification(
                refundRequest.getRequester().getId(),
                "Yêu cầu hoàn tiền bị từ chối",
                "Admin đã từ chối yêu cầu hoàn tiền của bạn. Hãy xem ghi chú để biết thêm chi tiết.",
                "{\"orderId\":\"" + order.getId() + "\",\"refundId\":\"" + refundRequest.getId() + "\"}"
        );
    }

    private void completeRefund(
            RefundRequest refundRequest,
            User currentUser,
            RefundReviewRequestDTO requestDTO
    ) {
        if (refundRequest.getStatus() != RefundStatus.approved) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        Payout payout = payoutService.ensureRefundPayout(refundRequest);
        payoutService.completeRefundPayout(payout, currentUser, requestDTO.getRefundReference(), requestDTO.getAdminNote());
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

    private void publishAdminRefundPendingNotification(Order order, RefundRequest refundRequest) {
        String metadata = "{\"orderId\":\"" + order.getId() + "\",\"refundId\":\"" + refundRequest.getId() + "\"}";
        String productTitle = order.getProduct() != null && order.getProduct().getTitle() != null
                ? order.getProduct().getTitle()
                : "đơn hàng";
        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        "Có yêu cầu hoàn tiền mới cần duyệt",
                        "Đơn \"" + productTitle + "\" vừa có yêu cầu hoàn tiền từ người mua.",
                        NotificationType.order,
                        metadata
                )));
    }

    private RefundResponseDTO mapToDTO(RefundRequest refundRequest) {
        return RefundResponseDTO.builder()
                .id(refundRequest.getId())
                .orderId(refundRequest.getOrder().getId())
                .paymentId(refundRequest.getPayment().getId())
                .requesterId(refundRequest.getRequester().getId())
                .requesterName(refundRequest.getRequester().getFullName())
                .amount(refundRequest.getAmount())
                .reason(refundRequest.getReason())
                .evidenceNote(refundRequest.getEvidenceNote())
                .status(refundRequest.getStatus())
                .adminNote(refundRequest.getAdminNote())
                .refundReference(refundRequest.getRefundReference())
                .reviewedBy(refundRequest.getReviewedBy() != null ? refundRequest.getReviewedBy().getId() : null)
                .reviewedByName(refundRequest.getReviewedBy() != null ? refundRequest.getReviewedBy().getFullName() : null)
                .reviewedAt(refundRequest.getReviewedAt())
                .processedAt(refundRequest.getProcessedAt())
                .createdAt(refundRequest.getCreatedAt())
                .build();
    }

    private AdminRefundResponseDTO mapToAdminDTO(
            RefundRequest refundRequest,
            boolean hasInspection,
            Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> evidenceByType
    ) {
        Order order = refundRequest.getOrder();
        Payment payment = refundRequest.getPayment();

        return AdminRefundResponseDTO.builder()
                .id(refundRequest.getId())
                .orderId(order != null ? order.getId() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .requesterId(refundRequest.getRequester().getId())
                .requesterName(refundRequest.getRequester().getFullName())
                .buyerId(order != null && order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order != null && order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .sellerId(order != null && order.getSeller() != null ? order.getSeller().getId() : null)
                .sellerName(order != null && order.getSeller() != null ? order.getSeller().getFullName() : null)
                .productId(order != null && order.getProduct() != null ? order.getProduct().getId() : null)
                .productTitle(order != null && order.getProduct() != null ? order.getProduct().getTitle() : null)
                .hasInspection(hasInspection)
                .amount(refundRequest.getAmount())
                .reason(refundRequest.getReason())
                .evidenceNote(refundRequest.getEvidenceNote())
                .status(refundRequest.getStatus())
                .adminNote(refundRequest.getAdminNote())
                .refundReference(refundRequest.getRefundReference())
                .reviewedBy(refundRequest.getReviewedBy() != null ? refundRequest.getReviewedBy().getId() : null)
                .reviewedByName(refundRequest.getReviewedBy() != null ? refundRequest.getReviewedBy().getFullName() : null)
                .reviewedAt(refundRequest.getReviewedAt())
                .processedAt(refundRequest.getProcessedAt())
                .createdAt(refundRequest.getCreatedAt())
                .sellerHandoverEvidence(evidenceByType.get(OrderEvidenceType.seller_handover))
                .buyerReceiptEvidence(evidenceByType.get(OrderEvidenceType.buyer_receipt))
                .paymentMethod(order != null ? order.getPaymentMethod() : null)
                .orderStatus(order != null ? order.getStatus() : null)
                .fundingStatus(order != null ? order.getFundingStatus() : null)
                .build();
    }
}
