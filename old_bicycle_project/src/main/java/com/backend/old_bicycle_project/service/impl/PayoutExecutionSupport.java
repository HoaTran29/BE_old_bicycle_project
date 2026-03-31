package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.entity.FinancialTransaction;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.FinancialTransactionEntryType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FinancialTransactionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.hasText;
import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.trimToNull;

final class PayoutExecutionSupport {

    private final PayoutRepository payoutRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaymentRepository paymentRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PayoutNotificationSupport payoutNotificationSupport;

    PayoutExecutionSupport(
            PayoutRepository payoutRepository,
            RefundRequestRepository refundRequestRepository,
            PaymentRepository paymentRepository,
            FinancialTransactionRepository financialTransactionRepository,
            OrderRepository orderRepository,
            ProductService productService,
            PayoutNotificationSupport payoutNotificationSupport
    ) {
        this.payoutRepository = payoutRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.paymentRepository = paymentRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.payoutNotificationSupport = payoutNotificationSupport;
    }

    Payout completeRefundPayout(Payout payout, User currentUser, String bankReference, String adminNote) {
        ensureCompletablePayout(payout, PayoutType.refund, bankReference);

        RefundRequest refundRequest = payout.getRefundRequest();
        if (refundRequest == null || refundRequest.getStatus() != RefundStatus.approved) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        Payment payment = refundRequest.getPayment();
        Order order = refundRequest.getOrder();

        markPayoutCompleted(payout, currentUser, bankReference, adminNote);

        refundRequest.setStatus(RefundStatus.completed);
        refundRequest.setAdminNote(adminNote);
        refundRequest.setRefundReference(bankReference);
        refundRequest.setReviewedBy(refundRequest.getReviewedBy() != null ? refundRequest.getReviewedBy() : currentUser);
        refundRequest.setReviewedAt(refundRequest.getReviewedAt() != null ? refundRequest.getReviewedAt() : LocalDateTime.now());
        refundRequest.setProcessedAt(LocalDateTime.now());

        payment.setStatus(PaymentStatus.refunded);

        order.setStatus(OrderStatus.cancelled);
        order.setFundingStatus(OrderFundingStatus.refunded);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(order.getTotalAmount());
        if (order.getPlatformFeeTotal() != null && order.getPlatformFeeTotal().compareTo(BigDecimal.ZERO) > 0) {
            order.setPlatformFeeStatus(PlatformFeeStatus.reversed);
            order.setPlatformFeeRecognizedAt(null);
            order.setPlatformFeeReversedAt(LocalDateTime.now());
        } else {
            order.setPlatformFeeStatus(PlatformFeeStatus.not_applicable);
            order.setPlatformFeeRecognizedAt(null);
            order.setPlatformFeeReversedAt(null);
        }
        productService.hideAfterRefundCompletion(order.getProduct());

        refundRequestRepository.save(refundRequest);
        paymentRepository.save(payment);
        orderRepository.save(order);
        payoutRepository.save(payout);
        recordFinancialTransaction(
                order,
                payment,
                payout,
                refundRequest,
                FinancialTransactionEntryType.buyer_fee_refund_completed,
                order.getBuyerFeeAmount(),
                "Hoàn lại phần phí buyer cho refund hợp lệ."
        );
        recordFinancialTransaction(
                order,
                payment,
                payout,
                refundRequest,
                FinancialTransactionEntryType.platform_fee_reversed,
                order.getPlatformFeeTotal(),
                "Đảo ngược doanh thu phí sàn vì refund hoàn tất."
        );

        payoutNotificationSupport.publishOrderNotification(
                refundRequest.getRequester().getId(),
                "Hoàn tiền đã được chuyển khoản",
                "Hệ thống đã ghi nhận giao dịch hoàn tiền thủ công cho yêu cầu của bạn.",
                "{\"refundId\":\"" + refundRequest.getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        payoutNotificationSupport.publishOrderNotification(
                order.getSeller().getId(),
                "Tin đăng đã bị ẩn sau khi hoàn tiền",
                "Admin đã hoàn tất hoàn tiền cho đơn hàng này. Tin đăng liên quan đã bị ẩn và cần cập nhật, duyệt lại, rồi kiểm định lại trước khi bán tiếp.",
                "{\"orderId\":\"" + order.getId() + "\",\"productId\":\"" + order.getProduct().getId() + "\"}"
        );

        return payout;
    }

    Payout completeSellerPayout(Payout payout, User currentUser, String bankReference, String adminNote) {
        ensureCompletablePayout(payout, PayoutType.seller_release, bankReference);

        Order order = payout.getOrder();
        if (order == null || order.getStatus() != OrderStatus.completed || order.getFundingStatus() != OrderFundingStatus.seller_payout_pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        markPayoutCompleted(payout, currentUser, bankReference, adminNote);

        order.setFundingStatus(OrderFundingStatus.released);
        if (order.getPlatformFeeTotal() != null && order.getPlatformFeeTotal().compareTo(BigDecimal.ZERO) > 0) {
            order.setPlatformFeeStatus(PlatformFeeStatus.recognized);
            order.setPlatformFeeRecognizedAt(LocalDateTime.now());
            order.setPlatformFeeReversedAt(null);
        } else {
            order.setPlatformFeeStatus(PlatformFeeStatus.not_applicable);
            order.setPlatformFeeRecognizedAt(null);
            order.setPlatformFeeReversedAt(null);
        }
        orderRepository.save(order);
        payoutRepository.save(payout);
        recordFinancialTransaction(
                order,
                null,
                payout,
                null,
                FinancialTransactionEntryType.seller_release_payout_completed,
                payout.getNetAmount(),
                "Admin hoàn tất payout cho seller."
        );
        recordFinancialTransaction(
                order,
                null,
                payout,
                null,
                FinancialTransactionEntryType.platform_fee_recognized,
                order.getPlatformFeeTotal(),
                "Ghi nhận doanh thu phí sàn khi payout seller hoàn tất."
        );

        payoutNotificationSupport.publishOrderNotification(
                order.getSeller().getId(),
                "Khoản cọc đã được giải ngân",
                "Hệ thống đã ghi nhận giao dịch chuyển khoản thủ công khoản cọc cho bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );

        return payout;
    }

    private void ensureCompletablePayout(Payout payout, PayoutType expectedType, String bankReference) {
        if (payout.getType() != expectedType) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        if (payout.getStatus() != PayoutStatus.pending_transfer) {
            throw new AppException(ErrorCode.PAYOUT_NOT_READY);
        }
        if (!hasText(bankReference)) {
            throw new AppException(ErrorCode.PAYOUT_REFERENCE_REQUIRED);
        }
    }

    private void markPayoutCompleted(Payout payout, User currentUser, String bankReference, String adminNote) {
        payout.setStatus(PayoutStatus.completed);
        payout.setBankReference(bankReference.trim());
        payout.setAdminNote(trimToNull(adminNote));
        payout.setCompletedBy(currentUser);
        payout.setCompletedAt(LocalDateTime.now());
    }

    private void recordFinancialTransaction(
            Order order,
            Payment payment,
            Payout payout,
            RefundRequest refundRequest,
            FinancialTransactionEntryType entryType,
            BigDecimal amount,
            String note
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        financialTransactionRepository.save(FinancialTransaction.builder()
                .order(order)
                .payment(payment)
                .payout(payout)
                .refundRequest(refundRequest)
                .entryType(entryType)
                .amount(amount)
                .note(note)
                .build());
    }
}
