package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminRefundResponseDTO;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.RefundService;
import com.backend.old_bicycle_project.specification.RefundRequestSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InspectionRepository inspectionRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            case completed -> completeRefund(refundRequest, order, payment, currentUser, requestDTO);
            default -> throw new AppException(ErrorCode.INVALID_STATUS);
        }

        refundRequestRepository.save(refundRequest);
        paymentRepository.save(payment);
        orderRepository.save(order);

        publishOrderNotification(
                refundRequest.getRequester().getId(),
                "Cập nhật yêu cầu hoàn tiền",
                "Admin đã cập nhật trạng thái yêu cầu hoàn tiền của bạn: " + refundRequest.getStatus().name(),
                "{\"orderId\":\"" + order.getId() + "\",\"refundId\":\"" + refundRequest.getId() + "\"}"
        );

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

        return refundPage.map(refundRequest -> mapToAdminDTO(
                refundRequest,
                refundRequest.getOrder() != null
                        && refundRequest.getOrder().getProduct() != null
                        && inspectedProductIds.contains(refundRequest.getOrder().getProduct().getId())
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
        order.setFundingStatus(OrderFundingStatus.refund_pending);
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
    }

    private void completeRefund(
            RefundRequest refundRequest,
            Order order,
            Payment payment,
            User currentUser,
            RefundReviewRequestDTO requestDTO
    ) {
        if (refundRequest.getStatus() != RefundStatus.approved) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        refundRequest.setStatus(RefundStatus.completed);
        refundRequest.setAdminNote(requestDTO.getAdminNote());
        refundRequest.setRefundReference(requestDTO.getRefundReference());
        refundRequest.setReviewedBy(currentUser);
        refundRequest.setReviewedAt(refundRequest.getReviewedAt() != null ? refundRequest.getReviewedAt() : LocalDateTime.now());
        refundRequest.setProcessedAt(LocalDateTime.now());

        payment.setStatus(PaymentStatus.refunded);

        order.setStatus(OrderStatus.cancelled);
        order.setFundingStatus(OrderFundingStatus.refunded);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(order.getTotalAmount());
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

    private AdminRefundResponseDTO mapToAdminDTO(RefundRequest refundRequest, boolean hasInspection) {
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
                .orderStatus(order != null ? order.getStatus() : null)
                .fundingStatus(order != null ? order.getFundingStatus() : null)
                .paymentMethod(order != null ? order.getPaymentMethod() : null)
                .build();
    }
}
