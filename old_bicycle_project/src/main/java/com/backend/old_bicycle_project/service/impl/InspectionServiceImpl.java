package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository inspectionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public InspectionResponseDTO requestInspection(UUID productId, UUID sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (product.getStatus() != ProductStatus.active
                && product.getStatus() != ProductStatus.inspected_failed
                && product.getStatus() != ProductStatus.inspected_passed) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        Inspection inspection = inspectionRepository.findByProductId(productId)
                .orElseGet(() -> Inspection.builder()
                        .product(product)
                        .build());

        inspection.setInspector(null);
        inspection.setOverallScore(null);
        inspection.setFrameScore(null);
        inspection.setForkScore(null);
        inspection.setBrakesScore(null);
        inspection.setDrivetrainScore(null);
        inspection.setWheelsScore(null);
        inspection.setWearPercentage(null);
        inspection.setExpertNotes(null);
        inspection.setPassed(false);
        inspection.setReportFileUrl(null);
        inspection.setValidUntil(null);
        inspection = inspectionRepository.save(inspection);

        product.setStatus(ProductStatus.pending_inspection);
        productRepository.save(product);

        return mapToDTO(inspection);
    }

    @Override
    @Transactional
    public InspectionResponseDTO evaluateInspection(UUID productId, UUID inspectorId, InspectionEvaluationDTO dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Inspection inspection = inspectionRepository.findByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (product.getStatus() != ProductStatus.pending_inspection) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        double averageScore = (dto.getFrameScore() + dto.getForkScore() + dto.getBrakesScore()
                + dto.getDrivetrainScore() + dto.getWheelsScore()) / 5.0;
        BigDecimal overallScore = BigDecimal.valueOf(averageScore).setScale(1, RoundingMode.HALF_UP);

        inspection.setInspector(inspector);
        inspection.setFrameScore(dto.getFrameScore());
        inspection.setForkScore(dto.getForkScore());
        inspection.setBrakesScore(dto.getBrakesScore());
        inspection.setDrivetrainScore(dto.getDrivetrainScore());
        inspection.setWheelsScore(dto.getWheelsScore());
        inspection.setWearPercentage(dto.getWearPercentage());
        inspection.setExpertNotes(dto.getExpertNotes());
        inspection.setOverallScore(overallScore);
        inspection.setPassed(dto.getPassed());
        inspection.setValidUntil(LocalDateTime.now().plusDays(7));
        inspection = inspectionRepository.save(inspection);

        product.setStatus(dto.getPassed() ? ProductStatus.inspected_passed : ProductStatus.inspected_failed);
        productRepository.save(product);

        return mapToDTO(inspection);
    }

    @Override
    public InspectionResponseDTO getInspectionByProductId(UUID productId) {
        Inspection inspection = inspectionRepository.findByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        return mapToDTO(inspection);
    }

    private InspectionResponseDTO mapToDTO(Inspection inspection) {
        return InspectionResponseDTO.builder()
                .id(inspection.getId())
                .productId(inspection.getProduct().getId())
                .inspectorId(inspection.getInspector() != null ? inspection.getInspector().getId() : null)
                .overallScore(inspection.getOverallScore())
                .frameScore(inspection.getFrameScore())
                .forkScore(inspection.getForkScore())
                .brakesScore(inspection.getBrakesScore())
                .drivetrainScore(inspection.getDrivetrainScore())
                .wheelsScore(inspection.getWheelsScore())
                .wearPercentage(inspection.getWearPercentage())
                .expertNotes(inspection.getExpertNotes())
                .passed(inspection.getPassed())
                .reportFileUrl(inspection.getReportFileUrl())
                .validUntil(inspection.getValidUntil())
                .createdAt(inspection.getCreatedAt())
                .build();
    }
}
