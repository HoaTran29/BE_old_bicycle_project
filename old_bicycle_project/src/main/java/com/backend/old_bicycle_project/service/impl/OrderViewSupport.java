package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class OrderViewSupport {

    private final ReviewRepository reviewRepository;
    private final OrderEvidenceService orderEvidenceService;

    OrderViewSupport(ReviewRepository reviewRepository, OrderEvidenceService orderEvidenceService) {
        this.reviewRepository = reviewRepository;
        this.orderEvidenceService = orderEvidenceService;
    }

    List<OrderResponseDTO> mapOrders(List<Order> orders) {
        Set<UUID> reviewedOrderIds = orders.isEmpty()
                ? Collections.emptySet()
                : reviewRepository.findReviewedOrderIdsByOrderIds(orders.stream().map(Order::getId).toList());
        Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> evidenceByOrder =
                orderEvidenceService.getEvidenceByOrderIds(orders.stream().map(Order::getId).toList());

        return orders.stream()
                .map(order -> mapToDTO(
                        order,
                        reviewedOrderIds.contains(order.getId()),
                        evidenceByOrder.getOrDefault(order.getId(), Collections.emptyMap())
                ))
                .toList();
    }

    OrderResponseDTO mapToDTO(Order order) {
        return mapToDTO(
                order,
                reviewRepository.existsByOrderId(order.getId()),
                orderEvidenceService.getEvidenceByOrderId(order.getId())
        );
    }

    OrderResponseDTO mapToDTO(
            Order order,
            Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> evidenceByType
    ) {
        return mapToDTO(order, reviewRepository.existsByOrderId(order.getId()), evidenceByType);
    }

    OrderResponseDTO mapToDTO(
            Order order,
            boolean buyerReviewSubmitted,
            Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> evidenceByType
    ) {
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
                .feeBaseAmount(order.getFeeBaseAmount())
                .platformFeeRate(order.getPlatformFeeRate())
                .platformFeeTotal(order.getPlatformFeeTotal())
                .buyerFeeAmount(order.getBuyerFeeAmount())
                .sellerFeeAmount(order.getSellerFeeAmount())
                .buyerChargeAmount(order.getBuyerChargeAmount())
                .sellerGrossPayoutAmount(order.getSellerGrossPayoutAmount())
                .sellerNetPayoutAmount(order.getSellerNetPayoutAmount())
                .platformFeeStatus(order.getPlatformFeeStatus())
                .platformFeeRecognizedAt(order.getPlatformFeeRecognizedAt())
                .platformFeeReversedAt(order.getPlatformFeeReversedAt())
                .paymentOption(order.getPaymentOption())
                .status(order.getStatus())
                .fundingStatus(order.getFundingStatus())
                .paymentMethod(order.getPaymentMethod())
                .buyerReviewSubmitted(buyerReviewSubmitted)
                .sellerHandoverEvidence(evidenceByType.get(OrderEvidenceType.seller_handover))
                .buyerReceiptEvidence(evidenceByType.get(OrderEvidenceType.buyer_receipt))
                .acceptedAt(order.getAcceptedAt())
                .paymentDeadline(order.getPaymentDeadline())
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
