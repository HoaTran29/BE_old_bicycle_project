package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.User;

import java.util.UUID;

public interface RefundService {

    RefundResponseDTO requestRefund(UUID orderId, User currentUser, RefundCreateRequestDTO requestDTO);

    RefundResponseDTO reviewRefund(UUID refundId, User currentUser, RefundReviewRequestDTO requestDTO);
}
