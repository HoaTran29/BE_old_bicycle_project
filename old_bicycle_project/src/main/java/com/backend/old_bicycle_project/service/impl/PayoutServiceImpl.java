package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.PayoutCompleteRequestDTO;
import com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminPayoutResponseDTO;
import com.backend.old_bicycle_project.dto.response.PayoutProfileResponseDTO;
import com.backend.old_bicycle_project.entity.FinancialTransaction;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.PayoutProfile;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.FinancialTransactionEntryType;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutProvider;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FinancialTransactionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PayoutProfileRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.PayoutService;
import com.backend.old_bicycle_project.service.ProductService;
import com.backend.old_bicycle_project.specification.PayoutSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutProfileRepository payoutProfileRepository;
    private final PayoutRepository payoutRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaymentRepository paymentRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductService productService;

    @Override
    @Transactional(readOnly = true)
    public PayoutProfileResponseDTO getMyProfile(User currentUser) {
        return payoutProfileRepository.findByUserId(currentUser.getId())
                .map(this::mapProfile)
                .orElse(null);
    }

    @Override
    @Transactional
    public PayoutProfileResponseDTO upsertMyProfile(User currentUser, PayoutProfileUpsertRequestDTO request) {
        PayoutProfile profile = payoutProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> PayoutProfile.builder().user(currentUser).build());

        profile.setBankCode(trimToNull(request.getBankCode()));
        profile.setBankBin(trimToNull(request.getBankBin()));
        profile.setAccountNumber(trimToNull(request.getAccountNumber()));
        profile.setAccountName(trimToNull(request.getAccountName()));
        PayoutProfile savedProfile = payoutProfileRepository.save(profile);

        hydratePendingPayouts(savedProfile);

        return mapProfile(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPayoutResponseDTO> getAdminPayouts(String keyword, PayoutType type, PayoutStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return payoutRepository.findAll(PayoutSpecification.fromAdminFilter(keyword, type, status), pageable)
                .map(this::mapAdminPayout);
    }

    @Override
    @Transactional
    public AdminPayoutResponseDTO completePayout(UUID payoutId, User currentUser, PayoutCompleteRequestDTO request) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (payout.getType() == PayoutType.refund) {
            return mapAdminPayout(completeRefundPayout(payout, currentUser, request.getBankReference(), request.getAdminNote()));
        }

        return mapAdminPayout(completeSellerPayout(payout, currentUser, request.getBankReference(), request.getAdminNote()));
    }

    @Override
    @Transactional
    public AdminPayoutResponseDTO remindProfileRequiredPayout(UUID payoutId, User currentUser) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (payout.getStatus() != PayoutStatus.profile_required) {
            throw new AppException(ErrorCode.PAYOUT_NOT_READY);
        }

        publishProfileRequiredNotification(payout, true);
        return mapAdminPayout(payout);
    }

    @Override
    @Transactional
    public Payout ensureRefundPayout(RefundRequest refundRequest) {
        return payoutRepository.findByRefundRequestId(refundRequest.getId())
                .map(existingPayout -> syncRefundPayout(existingPayout, refundRequest))
                .orElseGet(() -> createPayout(
                        refundRequest.getRequester(),
                        refundRequest.getOrder(),
                        refundRequest,
                        PayoutType.refund,
                        refundRequest.getAmount(),
                        BigDecimal.ZERO,
                        refundRequest.getAmount(),
                        buildTransferContent(PayoutType.refund, refundRequest.getId())
                ));
    }

    @Override
    @Transactional
    public Payout ensureSellerReleasePayout(Order order) {
        return payoutRepository.findByOrderIdAndType(order.getId(), PayoutType.seller_release)
                .map(existingPayout -> syncSellerReleasePayout(existingPayout, order))
                .orElseGet(() -> createPayout(
                        order.getSeller(),
                        order,
                        null,
                        PayoutType.seller_release,
                        resolveSellerGrossPayoutAmount(order),
                        resolveSellerFeeDeductionAmount(order),
                        resolveSellerNetPayoutAmount(order),
                        buildTransferContent(PayoutType.seller_release, order.getId())
                ));
    }

    @Override
    @Transactional
    public Payout completeRefundPayout(Payout payout, User currentUser, String bankReference, String adminNote) {
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

        publishOrderNotification(
                refundRequest.getRequester().getId(),
                "Hoàn tiền đã được chuyển khoản",
                "Hệ thống đã ghi nhận giao dịch hoàn tiền thủ công cho yêu cầu của bạn.",
                "{\"refundId\":\"" + refundRequest.getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Tin đăng đã bị ẩn sau khi hoàn tiền",
                "Admin đã hoàn tất hoàn tiền cho đơn hàng này. Tin đăng liên quan đã bị ẩn và cần cập nhật, duyệt lại, rồi kiểm định lại trước khi bán tiếp.",
                "{\"orderId\":\"" + order.getId() + "\",\"productId\":\"" + order.getProduct().getId() + "\"}"
        );

        return payout;
    }

    private Payout completeSellerPayout(Payout payout, User currentUser, String bankReference, String adminNote) {
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

        publishOrderNotification(
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

    private Payout syncPayoutWithCurrentProfile(Payout payout, User recipient) {
        if (payout.getStatus() == PayoutStatus.completed || payout.getStatus() == PayoutStatus.cancelled) {
            return payout;
        }

        payoutProfileRepository.findByUserId(recipient.getId()).ifPresent(profile -> applyProfileToPayout(payout, profile));
        return payoutRepository.save(payout);
    }

    private Payout syncRefundPayout(Payout payout, RefundRequest refundRequest) {
        payout.setAmount(refundRequest.getAmount());
        payout.setGrossAmount(refundRequest.getAmount());
        payout.setFeeDeductionAmount(BigDecimal.ZERO);
        payout.setNetAmount(refundRequest.getAmount());
        return syncPayoutWithCurrentProfile(payout, refundRequest.getRequester());
    }

    private Payout syncSellerReleasePayout(Payout payout, Order order) {
        payout.setAmount(resolveSellerNetPayoutAmount(order));
        payout.setGrossAmount(resolveSellerGrossPayoutAmount(order));
        payout.setFeeDeductionAmount(resolveSellerFeeDeductionAmount(order));
        payout.setNetAmount(resolveSellerNetPayoutAmount(order));
        return syncPayoutWithCurrentProfile(payout, order.getSeller());
    }

    private void hydratePendingPayouts(PayoutProfile profile) {
        List<Payout> pendingPayouts = payoutRepository.findByRecipientIdAndStatusOrderByCreatedAtAsc(
                profile.getUser().getId(),
                PayoutStatus.profile_required
        );
        if (pendingPayouts.isEmpty()) {
            return;
        }

        pendingPayouts.forEach(payout -> applyProfileToPayout(payout, profile));
        List<Payout> savedPayouts = payoutRepository.saveAll(pendingPayouts);
        savedPayouts.forEach(this::publishPayoutAwaitingNotification);
    }

    private Payout createPayout(
            User recipient,
            Order order,
            RefundRequest refundRequest,
            PayoutType type,
            BigDecimal grossAmount,
            BigDecimal feeDeductionAmount,
            BigDecimal netAmount,
            String transferContent
    ) {
        Payout payout = Payout.builder()
                .recipient(recipient)
                .order(order)
                .refundRequest(refundRequest)
                .type(type)
                .provider(PayoutProvider.vietqr_manual)
                .amount(netAmount)
                .grossAmount(grossAmount)
                .feeDeductionAmount(feeDeductionAmount)
                .netAmount(netAmount)
                .transferContent(transferContent)
                .status(PayoutStatus.profile_required)
                .build();

        payoutProfileRepository.findByUserId(recipient.getId()).ifPresent(profile -> applyProfileToPayout(payout, profile));

        Payout savedPayout = payoutRepository.save(payout);
        publishPayoutAwaitingNotification(savedPayout);
        return savedPayout;
    }

    private void applyProfileToPayout(Payout payout, PayoutProfile profile) {
        payout.setBankCode(profile.getBankCode());
        payout.setBankBin(profile.getBankBin());
        payout.setAccountNumber(profile.getAccountNumber());
        payout.setAccountName(profile.getAccountName());
        payout.setQrCodeUrl(buildQrCodeUrl(
                profile.getBankBin(),
                profile.getAccountNumber(),
                profile.getAccountName(),
                payout.getAmount(),
                payout.getTransferContent()
        ));
        payout.setStatus(PayoutStatus.pending_transfer);
    }

    private void publishPayoutAwaitingNotification(Payout payout) {
        if (payout.getType() == PayoutType.refund) {
            if (payout.getStatus() == PayoutStatus.profile_required) {
                publishProfileRequiredNotification(payout, false);
                return;
            }

            publishOrderNotification(
                    payout.getRecipient().getId(),
                    "Yêu cầu hoàn tiền đã được duyệt",
                    "Admin đã duyệt hoàn tiền. Hệ thống đang chờ chuyển khoản thủ công cho bạn.",
                    "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
            );
            publishAdminPayoutPendingNotification(
                    payout,
                    "Có payout hoàn tiền cần chuyển khoản",
                    "Một yêu cầu hoàn tiền đã được duyệt và đang chờ admin chuyển khoản thủ công."
            );
            return;
        }

        if (payout.getStatus() == PayoutStatus.profile_required) {
            publishProfileRequiredNotification(payout, false);
            return;
        }

        publishOrderNotification(
                payout.getRecipient().getId(),
                "Khoản cọc đang chờ giải ngân",
                "Đơn hàng đã hoàn tất và admin sẽ chuyển khoản thủ công khoản cọc cho bạn.",
                "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        publishAdminPayoutPendingNotification(
                payout,
                "Có payout cho người bán cần giải ngân",
                "Một đơn hàng đã hoàn tất và đang chờ admin giải ngân khoản cọc cho người bán."
        );
    }

    private void publishProfileRequiredNotification(Payout payout, boolean adminTriggeredReminder) {
        if (payout.getType() == PayoutType.refund) {
            publishOrderNotification(
                    payout.getRecipient().getId(),
                    adminTriggeredReminder
                            ? "Admin nhắc cập nhật tài khoản nhận hoàn tiền"
                            : "Cần cập nhật tài khoản nhận hoàn tiền",
                    adminTriggeredReminder
                            ? "Admin đang chờ bạn cập nhật payout profile để có thể chuyển khoản hoàn tiền."
                            : "Yêu cầu hoàn tiền đã được duyệt nhưng bạn cần cập nhật payout profile để admin chuyển khoản.",
                    "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
            );
            publishAdminPayoutProfileRequiredNotification(
                    payout,
                    "Payout hoàn tiền đang bị chặn vì thiếu payout profile",
                    "Người mua chưa cập nhật payout profile nên admin chưa thể hoàn tiền thủ công."
            );
            return;
        }

        publishOrderNotification(
                payout.getRecipient().getId(),
                adminTriggeredReminder
                        ? "Admin nhắc cập nhật tài khoản nhận giải ngân"
                        : "Cần cập nhật tài khoản nhận giải ngân",
                adminTriggeredReminder
                        ? "Admin đang chờ bạn cập nhật payout profile để có thể giải ngân khoản cọc."
                        : "Đơn hàng đã hoàn tất nhưng bạn cần cập nhật payout profile trước khi nhận khoản cọc.",
                "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        publishAdminPayoutProfileRequiredNotification(
                payout,
                "Payout giải ngân đang bị chặn vì thiếu payout profile",
                "Người bán chưa cập nhật payout profile nên admin chưa thể giải ngân khoản cọc."
        );
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

    private void publishAdminPayoutPendingNotification(Payout payout, String title, String content) {
        String metadata = buildPayoutMetadata(payout);

        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        title,
                        content,
                        NotificationType.order,
                        metadata
                )));
    }

    private void publishAdminPayoutProfileRequiredNotification(Payout payout, String title, String content) {
        String metadata = buildPayoutMetadata(payout);

        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        title,
                        content,
                        NotificationType.order,
                        metadata
                )));
    }

    private String buildPayoutMetadata(Payout payout) {
        return payout.getType() == PayoutType.refund
                ? "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
                : "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompleteProfile(User user) {
        return payoutProfileRepository.findByUserId(user.getId())
                .map(this::isProfileComplete)
                .orElse(false);
    }

    private boolean isProfileComplete(PayoutProfile profile) {
        return hasText(profile.getBankCode())
                && hasText(profile.getBankBin())
                && hasText(profile.getAccountNumber())
                && hasText(profile.getAccountName());
    }

    private PayoutProfileResponseDTO mapProfile(PayoutProfile profile) {
        return PayoutProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .bankCode(profile.getBankCode())
                .bankBin(profile.getBankBin())
                .accountNumber(profile.getAccountNumber())
                .accountName(profile.getAccountName())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private AdminPayoutResponseDTO mapAdminPayout(Payout payout) {
        Order order = payout.getOrder();
        RefundRequest refundRequest = payout.getRefundRequest();

        return AdminPayoutResponseDTO.builder()
                .id(payout.getId())
                .type(payout.getType())
                .status(payout.getStatus())
                .provider(payout.getProvider())
                .amount(payout.getAmount())
                .grossAmount(payout.getGrossAmount() != null ? payout.getGrossAmount() : payout.getAmount())
                .feeDeductionAmount(payout.getFeeDeductionAmount() != null ? payout.getFeeDeductionAmount() : BigDecimal.ZERO)
                .netAmount(payout.getNetAmount() != null ? payout.getNetAmount() : payout.getAmount())
                .recipientId(payout.getRecipient().getId())
                .recipientName(payout.getRecipient().getFullName())
                .bankCode(payout.getBankCode())
                .bankBin(payout.getBankBin())
                .accountNumber(payout.getAccountNumber())
                .accountName(payout.getAccountName())
                .transferContent(payout.getTransferContent())
                .qrCodeUrl(payout.getQrCodeUrl())
                .bankReference(payout.getBankReference())
                .adminNote(payout.getAdminNote())
                .orderId(order != null ? order.getId() : null)
                .orderStatus(order != null ? order.getStatus() : null)
                .fundingStatus(order != null ? order.getFundingStatus() : null)
                .refundRequestId(refundRequest != null ? refundRequest.getId() : null)
                .productId(order != null && order.getProduct() != null ? order.getProduct().getId() : null)
                .productTitle(order != null && order.getProduct() != null ? order.getProduct().getTitle() : null)
                .buyerId(order != null && order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order != null && order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .sellerId(order != null && order.getSeller() != null ? order.getSeller().getId() : null)
                .sellerName(order != null && order.getSeller() != null ? order.getSeller().getFullName() : null)
                .completedById(payout.getCompletedBy() != null ? payout.getCompletedBy().getId() : null)
                .completedByName(payout.getCompletedBy() != null ? payout.getCompletedBy().getFullName() : null)
                .completedAt(payout.getCompletedAt())
                .createdAt(payout.getCreatedAt())
                .build();
    }

    private BigDecimal resolveSellerGrossPayoutAmount(Order order) {
        if (order.getSellerGrossPayoutAmount() != null && order.getSellerGrossPayoutAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getSellerGrossPayoutAmount();
        }
        if (order.getRequiredUpfrontAmount() != null && order.getRequiredUpfrontAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getRequiredUpfrontAmount();
        }
        if (order.getDepositAmount() != null && order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getDepositAmount();
        }
        if (order.getPaidAmount() != null && order.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getPaidAmount();
        }
        throw new AppException(ErrorCode.PAYOUT_NOT_READY);
    }

    private BigDecimal resolveSellerFeeDeductionAmount(Order order) {
        return order.getSellerFeeAmount() != null ? order.getSellerFeeAmount() : BigDecimal.ZERO;
    }

    private BigDecimal resolveSellerNetPayoutAmount(Order order) {
        if (order.getSellerNetPayoutAmount() != null && order.getSellerNetPayoutAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getSellerNetPayoutAmount();
        }
        BigDecimal grossAmount = resolveSellerGrossPayoutAmount(order);
        BigDecimal feeDeductionAmount = resolveSellerFeeDeductionAmount(order);
        BigDecimal netAmount = grossAmount.subtract(feeDeductionAmount);
        return netAmount.compareTo(BigDecimal.ZERO) > 0 ? netAmount : BigDecimal.ZERO;
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

    private String buildTransferContent(PayoutType type, UUID referenceId) {
        String compactReference = referenceId.toString().replace("-", "").substring(0, 12).toUpperCase();
        return (type == PayoutType.refund ? "RF-" : "SL-") + compactReference;
    }

    private String buildQrCodeUrl(
            String bankBin,
            String accountNumber,
            String accountName,
            BigDecimal amount,
            String transferContent
    ) {
        if (!hasText(bankBin) || !hasText(accountNumber) || amount == null) {
            return null;
        }

        String safeAccountName = accountName != null ? accountName : "";
        return "https://img.vietqr.io/image/"
                + bankBin
                + "-"
                + accountNumber
                + "-compact2.png?amount="
                + amount.toPlainString()
                + "&addInfo="
                + URLEncoder.encode(transferContent, StandardCharsets.UTF_8)
                + "&accountName="
                + URLEncoder.encode(safeAccountName, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
