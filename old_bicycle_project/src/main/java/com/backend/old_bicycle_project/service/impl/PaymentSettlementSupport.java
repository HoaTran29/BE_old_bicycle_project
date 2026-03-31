package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.entity.FinancialTransaction;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.enums.FinancialTransactionEntryType;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderCancelReason;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FinancialTransactionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.PayoutService;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.firstNonBlank;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.maxZero;

final class PaymentSettlementSupport {

    static final List<PaymentStatus> OPEN_UPFRONT_PAYMENT_STATUSES =
            List.of(PaymentStatus.pending, PaymentStatus.processing);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PayoutService payoutService;

    PaymentSettlementSupport(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            RefundRequestRepository refundRequestRepository,
            FinancialTransactionRepository financialTransactionRepository,
            ApplicationEventPublisher eventPublisher,
            PayoutService payoutService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.eventPublisher = eventPublisher;
        this.payoutService = payoutService;
    }

    void confirmSuccessfulPayment(
            Payment payment,
            BigDecimal actualAmount,
            String transactionReference,
            LocalDateTime paymentDate,
            String gatewayPayload
    ) {
        if (payment.getStatus() == PaymentStatus.success) {
            return;
        }
        if (actualAmount == null || actualAmount.compareTo(payment.getAmount()) < 0) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        payment.setStatus(PaymentStatus.success);
        payment.setTransactionReference(firstNonBlank(transactionReference, UUID.randomUUID().toString()));
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDateTime.now());
        payment.setGatewayResponse(gatewayPayload);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal protectedAmount = resolveProtectedAmount(payment);
        BigDecimal newPaid = currentPaid.add(protectedAmount);
        order.setPaidAmount(newPaid);
        order.setRemainingAmount(maxZero(order.getTotalAmount().subtract(newPaid)));
        if (order.getPlatformFeeTotal() != null && order.getPlatformFeeTotal().compareTo(BigDecimal.ZERO) > 0) {
            order.setPlatformFeeStatus(PlatformFeeStatus.pending);
        }
        if (order.getStatus() == OrderStatus.cancelled) {
            handleLatePaymentForCancelledOrder(order, payment);
            return;
        }
        if (newPaid.compareTo(order.getRequiredUpfrontAmount()) >= 0) {
            order.setStatus(OrderStatus.deposited);
            order.setFundingStatus(OrderFundingStatus.held);
        }
        orderRepository.save(order);
        recordFinancialTransaction(
                order,
                payment,
                null,
                null,
                FinancialTransactionEntryType.buyer_charge_received,
                resolveChargeAmount(payment),
                "Buyer thanh toán thành công cho payment phase hiện tại."
        );

        publishOrderNotification(
                order.getBuyer().getId(),
                "Thanh toán đặt cọc thành công",
                "Hệ thống đã ghi nhận khoản thanh toán cho order của bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Order đã được thanh toán tiền đặt cọc",
                "Người mua đã thanh toán thành công khoản ứng trước cho order.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
    }

    void expireOrderDueToPaymentTimeout(Order order, LocalDateTime now) {
        List<Payment> openPayments = paymentRepository.findByOrderIdAndPhaseAndStatusIn(
                order.getId(),
                PaymentPhase.upfront,
                OPEN_UPFRONT_PAYMENT_STATUSES
        );
        expireOrderDueToPaymentTimeout(order, now, openPayments);
    }

    void expireOrderDueToPaymentTimeout(Order order, LocalDateTime now, List<Payment> openPayments) {
        if (order.getStatus() == OrderStatus.cancelled && order.getCancelReason() == OrderCancelReason.payment_expired) {
            return;
        }

        order.setStatus(OrderStatus.cancelled);
        order.setFundingStatus(OrderFundingStatus.unpaid);
        order.setCancelReason(OrderCancelReason.payment_expired);
        order.setCancelledAt(now);
        if (order.getPlatformFeeStatus() == PlatformFeeStatus.pending) {
            order.setPlatformFeeStatus(PlatformFeeStatus.not_applicable);
            order.setPlatformFeeRecognizedAt(null);
            order.setPlatformFeeReversedAt(null);
        }
        orderRepository.save(order);

        if (!openPayments.isEmpty()) {
            LocalDateTime expiresAt = order.getPaymentDeadline() != null ? order.getPaymentDeadline() : now;
            openPayments.forEach(payment -> {
                payment.setStatus(PaymentStatus.expired);
                payment.setExpiresAt(expiresAt);
            });
            paymentRepository.saveAll(openPayments);
        }

        publishOrderNotification(
                order.getBuyer().getId(),
                "Đơn hàng đã hết hạn thanh toán",
                "Bạn chưa hoàn tất thanh toán đúng hạn nên hệ thống đã tự hủy đơn hàng này.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Đơn hàng tự hủy vì quá hạn thanh toán",
                "Người mua chưa thanh toán đúng hạn nên hệ thống đã tự hủy đơn hàng này.",
                "{\"orderId\":\"" + order.getId() + "\"}"
        );
    }

    private void handleLatePaymentForCancelledOrder(Order order, Payment payment) {
        BigDecimal protectedAmount = resolveProtectedAmount(payment);
        BigDecimal chargeAmount = resolveChargeAmount(payment);
        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : protectedAmount;
        order.setPaidAmount(paidAmount);
        order.setRemainingAmount(maxZero(order.getTotalAmount().subtract(paidAmount)));
        order.setFundingStatus(OrderFundingStatus.refund_pending_transfer);
        if (order.getPlatformFeeTotal() != null && order.getPlatformFeeTotal().compareTo(BigDecimal.ZERO) > 0) {
            order.setPlatformFeeStatus(PlatformFeeStatus.pending);
        }

        RefundRequest refundRequest = refundRequestRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .orElseGet(() -> RefundRequest.builder()
                        .order(order)
                        .payment(payment)
                        .requester(order.getBuyer())
                        .amount(chargeAmount)
                        .reason("Hệ thống tự tạo hoàn tiền vì thanh toán đến sau khi đơn hàng đã bị hủy.")
                        .evidenceNote("Late payment received after order cancellation/expiry.")
                        .status(RefundStatus.approved)
                        .adminNote("Tự động duyệt hoàn tiền vì hệ thống nhận thanh toán sau khi đơn đã hết hạn hoặc bị hủy.")
                        .reviewedAt(LocalDateTime.now())
                        .build());

        refundRequest.setPayment(payment);
        refundRequest.setAmount(chargeAmount);
        refundRequest.setStatus(RefundStatus.approved);
        refundRequest.setAdminNote(
                "Tự động duyệt hoàn tiền vì hệ thống nhận thanh toán sau khi đơn đã hết hạn hoặc bị hủy."
        );
        refundRequest.setReviewedAt(refundRequest.getReviewedAt() != null ? refundRequest.getReviewedAt() : LocalDateTime.now());

        refundRequest = refundRequestRepository.save(refundRequest);
        orderRepository.save(order);
        payoutService.ensureRefundPayout(refundRequest);

        publishOrderNotification(
                order.getBuyer().getId(),
                "Thanh toán đến muộn, hệ thống sẽ hoàn tiền",
                "Hệ thống nhận được khoản thanh toán sau khi đơn đã hết hạn hoặc bị hủy. Khoản tiền này sẽ được hoàn thủ công cho bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Đơn hàng nhận thanh toán muộn sau khi đã hủy",
                "Hệ thống đã nhận được thanh toán sau khi đơn bị hủy. Khoản tiền này sẽ được hoàn lại cho người mua, đơn hàng không được khôi phục.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
    }

    private BigDecimal resolveProtectedAmount(Payment payment) {
        if (payment.getProtectedAmount() != null && payment.getProtectedAmount().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getProtectedAmount();
        }
        return payment.getAmount();
    }

    private BigDecimal resolveChargeAmount(Payment payment) {
        if (payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getAmount();
        }
        BigDecimal protectedAmount = resolveProtectedAmount(payment);
        return protectedAmount != null ? protectedAmount : BigDecimal.ZERO;
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
}
