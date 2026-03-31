package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.PayoutProfile;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.PayoutProvider;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.PayoutProfileRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.buildQrCodeUrl;
import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.buildTransferContent;
import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.hasText;
import static com.backend.old_bicycle_project.service.impl.PayoutSupportUtils.trimToNull;

final class PayoutWorkflowSupport {

    private final PayoutProfileRepository payoutProfileRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutNotificationSupport payoutNotificationSupport;

    PayoutWorkflowSupport(
            PayoutProfileRepository payoutProfileRepository,
            PayoutRepository payoutRepository,
            PayoutNotificationSupport payoutNotificationSupport
    ) {
        this.payoutProfileRepository = payoutProfileRepository;
        this.payoutRepository = payoutRepository;
        this.payoutNotificationSupport = payoutNotificationSupport;
    }

    Optional<PayoutProfile> findProfile(User currentUser) {
        return payoutProfileRepository.findByUserId(currentUser.getId());
    }

    PayoutProfile upsertMyProfile(User currentUser, PayoutProfileUpsertRequestDTO request) {
        PayoutProfile profile = payoutProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> PayoutProfile.builder().user(currentUser).build());

        profile.setBankCode(trimToNull(request.getBankCode()));
        profile.setBankBin(trimToNull(request.getBankBin()));
        profile.setAccountNumber(trimToNull(request.getAccountNumber()));
        profile.setAccountName(trimToNull(request.getAccountName()));
        PayoutProfile savedProfile = payoutProfileRepository.save(profile);

        hydratePendingPayouts(savedProfile);
        return savedProfile;
    }

    boolean hasCompleteProfile(User user) {
        return payoutProfileRepository.findByUserId(user.getId())
                .map(this::isProfileComplete)
                .orElse(false);
    }

    Payout ensureRefundPayout(RefundRequest refundRequest) {
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

    Payout ensureSellerReleasePayout(Order order) {
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
        savedPayouts.forEach(payoutNotificationSupport::publishPayoutAwaitingNotification);
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
        payoutNotificationSupport.publishPayoutAwaitingNotification(savedPayout);
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

    private boolean isProfileComplete(PayoutProfile profile) {
        return hasText(profile.getBankCode())
                && hasText(profile.getBankBin())
                && hasText(profile.getAccountNumber())
                && hasText(profile.getAccountName());
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
}
