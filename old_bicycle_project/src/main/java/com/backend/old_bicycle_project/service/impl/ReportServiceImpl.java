package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReportRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ReportResponseDTO submitReport(UUID reporterId, ReportRequestDTO requestDTO) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Basic validation for target existence
        if ("USER".equalsIgnoreCase(requestDTO.getTargetType())) {
            if (!userRepository.existsById(requestDTO.getTargetId())) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }
        } else if ("PRODUCT".equalsIgnoreCase(requestDTO.getTargetType())) {
            if (!productRepository.existsById(requestDTO.getTargetId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        } else {
            throw new IllegalArgumentException("Invalid target type");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetId(requestDTO.getTargetId())
                .targetType(requestDTO.getTargetType().toUpperCase())
                .reason(requestDTO.getReason())
                .description(requestDTO.getDescription())
                .status(ReportStatus.pending)
                .build();

        report = reportRepository.save(report);
        return mapToDTO(report);
    }

    @Override
    public Page<ReportResponseDTO> getAllReports(Pageable pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public ReportResponseDTO processReport(UUID reportId, ReportProcessDTO processDTO) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        // Update the report status
        report.setStatus(processDTO.getStatus());
        
        // If RESOLVED, apply actions to the target entity
        if (processDTO.getStatus() == ReportStatus.resolved) {
            applySanctions(report.getTargetType(), report.getTargetId());
        }

        report = reportRepository.save(report);
        return mapToDTO(report);
    }

    private void applySanctions(String targetType, UUID targetId) {
        try {
            if ("USER".equalsIgnoreCase(targetType)) {
                User user = userRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                user.setStatus(UserStatus.banned);
                userRepository.save(user);
                log.info("Banned user with ID: {}", targetId);
            } else if ("PRODUCT".equalsIgnoreCase(targetType)) {
                Product product = productRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                product.setStatus(ProductStatus.hidden);
                productRepository.save(product);
                log.info("Hidden product with ID: {}", targetId);
            }
        } catch (Exception e) {
            log.error("Failed to apply sanctions for targetType: {}, targetId: {}", targetType, targetId, e);
            throw new AppException(ErrorCode.INVALID_STATUS); // Or another appropriate error
        }
    }

    private ReportResponseDTO mapToDTO(Report report) {
        return ReportResponseDTO.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
