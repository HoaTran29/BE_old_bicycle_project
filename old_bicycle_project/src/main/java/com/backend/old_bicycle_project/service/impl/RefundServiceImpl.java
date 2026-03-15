package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
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
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RefundResponseDTO requestRefund(UUID orderId, User currentUser, RefundCreateRequestDTO requestDTO) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (order.getStatus() != OrderStatus.deposited || order.getFundingStatus() != OrderFundingStatus.held) {
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
                "Da tao yeu cau hoan tien",
                "Yeu cau hoan tien cua ban da duoc gui cho admin xem xet.",
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
                "Cap nhat yeu cau hoan tien",
                "Admin da cap nhat trang thai yeu cau hoan tien cua ban: " + refundRequest.getStatus().name(),
                "{\"orderId\":\"" + order.getId() + "\",\"refundId\":\"" + refundRequest.getId() + "\"}"
        );

        return mapToDTO(refundRequest);
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
}
