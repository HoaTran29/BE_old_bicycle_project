package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderEvidenceService {

    OrderEvidenceSubmissionResponseDTO createSellerHandoverEvidence(
            Order order,
            User submittedBy,
            String note,
            List<MultipartFile> files
    );

    OrderEvidenceSubmissionResponseDTO createBuyerReceiptEvidence(
            Order order,
            User submittedBy,
            String note,
            List<MultipartFile> files
    );

    Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> getEvidenceByOrderIds(Collection<UUID> orderIds);

    Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> getEvidenceByOrderId(UUID orderId);
}
