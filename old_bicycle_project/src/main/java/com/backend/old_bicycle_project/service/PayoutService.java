package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.PayoutCompleteRequestDTO;
import com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminPayoutResponseDTO;
import com.backend.old_bicycle_project.dto.response.PayoutProfileResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PayoutService {
    PayoutProfileResponseDTO getMyProfile(User currentUser);

    PayoutProfileResponseDTO upsertMyProfile(User currentUser, PayoutProfileUpsertRequestDTO request);

    Page<AdminPayoutResponseDTO> getAdminPayouts(String keyword, PayoutType type, PayoutStatus status, int page, int size);

    AdminPayoutResponseDTO completePayout(UUID payoutId, User currentUser, PayoutCompleteRequestDTO request);

    Payout ensureRefundPayout(RefundRequest refundRequest);

    Payout ensureSellerReleasePayout(Order order);

    Payout completeRefundPayout(Payout payout, User currentUser, String bankReference, String adminNote);
}
