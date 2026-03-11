package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;

import java.util.UUID;

public interface InspectionService {

    /**
     * Seller requests an inspection for their product.
     * @param productId UUID of the product
     * @param sellerId UUID of the authenticated seller
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO requestInspection(UUID productId, UUID sellerId);

    /**
     * Inspector evaluates the product and submits the scores.
     * @param productId UUID of the product
     * @param inspectorId UUID of the authenticated inspector
     * @param evaluationDTO Evaluation data
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO evaluateInspection(UUID productId, UUID inspectorId, InspectionEvaluationDTO evaluationDTO);

    /**
     * Get inspection details by product ID.
     * @param productId UUID of the product
     * @return InspectionResponseDTO
     */
    InspectionResponseDTO getInspectionByProductId(UUID productId);
}
