package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.PayoutCompleteRequestDTO;
import com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminPayoutResponseDTO;
import com.backend.old_bicycle_project.dto.response.PayoutProfileResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.PayoutProfile;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
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
import com.backend.old_bicycle_project.validation.PaginationValidationUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutNotificationSupport payoutNotificationSupport;
    private final PayoutWorkflowSupport payoutWorkflowSupport;
    private final PayoutExecutionSupport payoutExecutionSupport;

    public PayoutServiceImpl(
            PayoutProfileRepository payoutProfileRepository,
            PayoutRepository payoutRepository,
            RefundRequestRepository refundRequestRepository,
            PaymentRepository paymentRepository,
            FinancialTransactionRepository financialTransactionRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            ProductService productService
    ) {
        this.payoutRepository = payoutRepository;
        this.payoutNotificationSupport = new PayoutNotificationSupport(eventPublisher, userRepository);
        this.payoutWorkflowSupport = new PayoutWorkflowSupport(
                payoutProfileRepository,
                payoutRepository,
                payoutNotificationSupport
        );
        this.payoutExecutionSupport = new PayoutExecutionSupport(
                payoutRepository,
                refundRequestRepository,
                paymentRepository,
                financialTransactionRepository,
                orderRepository,
                productService,
                payoutNotificationSupport
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutProfileResponseDTO getMyProfile(User currentUser) {
        return payoutWorkflowSupport.findProfile(currentUser)
                .map(this::mapProfile)
                .orElse(null);
    }

    @Override
    @Transactional
    public PayoutProfileResponseDTO upsertMyProfile(User currentUser, PayoutProfileUpsertRequestDTO request) {
        return mapProfile(payoutWorkflowSupport.upsertMyProfile(currentUser, request));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPayoutResponseDTO> getAdminPayouts(String keyword, PayoutType type, PayoutStatus status, int page, int size) {
        var pageable = PaginationValidationUtils.createPageRequest(page, size, Sort.by("createdAt").descending());
        return payoutRepository.findAll(PayoutSpecification.fromAdminFilter(keyword, type, status), pageable)
                .map(this::mapAdminPayout);
    }

    @Override
    @Transactional
    public AdminPayoutResponseDTO completePayout(UUID payoutId, User currentUser, PayoutCompleteRequestDTO request) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (payout.getType() == PayoutType.refund) {
            return mapAdminPayout(
                    payoutExecutionSupport.completeRefundPayout(
                            payout,
                            currentUser,
                            request.getBankReference(),
                            request.getAdminNote()
                    )
            );
        }

        return mapAdminPayout(
                payoutExecutionSupport.completeSellerPayout(
                        payout,
                        currentUser,
                        request.getBankReference(),
                        request.getAdminNote()
                )
        );
    }

    @Override
    @Transactional
    public AdminPayoutResponseDTO remindProfileRequiredPayout(UUID payoutId, User currentUser) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (payout.getStatus() != PayoutStatus.profile_required) {
            throw new AppException(ErrorCode.PAYOUT_NOT_READY);
        }

        payoutNotificationSupport.publishProfileRequiredNotification(payout, true);
        return mapAdminPayout(payout);
    }

    @Override
    @Transactional
    public Payout ensureRefundPayout(RefundRequest refundRequest) {
        return payoutWorkflowSupport.ensureRefundPayout(refundRequest);
    }

    @Override
    @Transactional
    public Payout ensureSellerReleasePayout(Order order) {
        return payoutWorkflowSupport.ensureSellerReleasePayout(order);
    }

    @Override
    @Transactional
    public Payout completeRefundPayout(Payout payout, User currentUser, String bankReference, String adminNote) {
        return payoutExecutionSupport.completeRefundPayout(payout, currentUser, bankReference, adminNote);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompleteProfile(User user) {
        return payoutWorkflowSupport.hasCompleteProfile(user);
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
}
