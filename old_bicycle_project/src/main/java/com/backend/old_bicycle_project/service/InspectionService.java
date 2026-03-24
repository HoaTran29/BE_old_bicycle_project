package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionHistoryItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionRequestItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface InspectionService {

    /**
     * Admin routes a listing to the inspection queue before it can become public.
     *
     * @param productId UUID of the product
     * @param moderatorId UUID of the authenticated admin
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO requestInspection(UUID productId, UUID moderatorId);

    /**
     * Inspector evaluates the product and submits the scores.
     * @param productId UUID of the product
     * @param inspectorId UUID of the authenticated inspector
     * @param evaluationDTO Evaluation data
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO evaluateInspection(UUID productId, UUID inspectorId, InspectionEvaluationDTO evaluationDTO);

    InspectionResponseDTO uploadInspectionReport(UUID productId, UUID inspectorId, MultipartFile reportFile);

    /**
     * Get inspection details by product ID.
     * @param productId UUID of the product
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO getInspectionByProductId(UUID productId);

    Page<InspectionRequestItemResponseDTO> getInspectionRequests(String keyword, int page, int size);

    Page<InspectionHistoryItemResponseDTO> getInspectionHistory(User currentUser, String keyword, int page, int size);

    InspectionDashboardResponseDTO getInspectionDashboard(User currentUser);
}
