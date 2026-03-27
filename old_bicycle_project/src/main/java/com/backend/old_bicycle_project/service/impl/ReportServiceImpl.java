package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReportEvidenceFileResponseDTO;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.ReportFile;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReportRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.ReportService;
import com.backend.old_bicycle_project.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final List<ReportStatus> OPEN_REPORT_STATUSES = List.of(
            ReportStatus.pending,
            ReportStatus.investigating
    );
    private static final Set<ReportStatus> TERMINAL_REPORT_STATUSES = EnumSet.of(
            ReportStatus.resolved_upheld,
            ReportStatus.resolved_dismissed
    );
    private static final int MAX_REPORT_EVIDENCE_FILES = 3;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;

    @Override
    @Transactional
    public ReportResponseDTO submitReport(UUID reporterId, ReportRequestDTO requestDTO, List<MultipartFile> files) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateTargetExists(requestDTO.getTargetType(), requestDTO.getTargetId());

        if (reportRepository.existsByReporterIdAndTargetIdAndStatusIn(
                reporterId,
                requestDTO.getTargetId(),
                OPEN_REPORT_STATUSES
        )) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Report report = reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetId(requestDTO.getTargetId())
                .targetType(normalizeTargetType(requestDTO.getTargetType()))
                .reason(requestDTO.getReason())
                .description(trimToNull(requestDTO.getDescription()))
                .status(ReportStatus.pending)
                .build());

        report = attachEvidenceFiles(report, files);
        publishPendingReportNotification(report);
        return mapToDTO(report);
    }

    @Override
    public Page<ReportResponseDTO> getAllReports(ReportStatus status, String targetType, Pageable pageable) {
        Specification<Report> specification = Specification.where(null);

        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (targetType != null && !targetType.isBlank()) {
            String normalizedTargetType = normalizeTargetType(targetType);
            specification = specification.and((root, query, cb) -> cb.equal(root.get("targetType"), normalizedTargetType));
        }

        return reportRepository.findAll(specification, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<ReportResponseDTO> getMyReports(UUID reporterId, Pageable pageable) {
        if (!userRepository.existsById(reporterId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public ReportResponseDTO processReport(UUID reportId, ReportProcessDTO processDTO, UUID adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateProcessTransition(report.getStatus(), processDTO.getStatus());

        report.setStatus(processDTO.getStatus());
        report.setAdminNote(trimToNull(processDTO.getAdminNote()));
        report.setProcessedAt(LocalDateTime.now());
        report.setProcessedBy(admin);

        UUID affectedUserId = null;
        if (processDTO.getStatus() == ReportStatus.resolved_upheld) {
            affectedUserId = applySanctions(report.getTargetType(), report.getTargetId());
        }

        report = reportRepository.save(report);
        publishReportNotifications(report, affectedUserId);
        return mapToDTO(report);
    }

    private void validateTargetExists(String targetType, UUID targetId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        if ("USER".equals(normalizedTargetType)) {
            if (!userRepository.existsById(targetId)) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }
            return;
        }
        if ("PRODUCT".equals(normalizedTargetType)) {
            if (!productRepository.existsById(targetId)) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return;
        }
        throw new AppException(ErrorCode.INVALID_KEY);
    }

    private void validateProcessTransition(ReportStatus currentStatus, ReportStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            throw new AppException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION);
        }

        if (TERMINAL_REPORT_STATUSES.contains(currentStatus) || currentStatus == nextStatus) {
            throw new AppException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION);
        }

        boolean isValidTransition = switch (currentStatus) {
            case pending -> nextStatus == ReportStatus.investigating
                    || nextStatus == ReportStatus.resolved_upheld
                    || nextStatus == ReportStatus.resolved_dismissed;
            case investigating -> nextStatus == ReportStatus.resolved_upheld
                    || nextStatus == ReportStatus.resolved_dismissed;
            default -> false;
        };

        if (!isValidTransition) {
            throw new AppException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION);
        }
    }

    private UUID applySanctions(String targetType, UUID targetId) {
        try {
            if ("USER".equalsIgnoreCase(targetType)) {
                User user = userRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                user.setStatus(UserStatus.banned);
                userRepository.save(user);
                log.info("Banned user with ID: {}", targetId);
                return user.getId();
            }
            if ("PRODUCT".equalsIgnoreCase(targetType)) {
                Product product = productRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                product.setStatus(ProductStatus.hidden);
                productRepository.save(product);
                log.info("Hidden product with ID: {}", targetId);
                return product.getSeller() != null ? product.getSeller().getId() : null;
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to apply sanctions for targetType: {}, targetId: {}", targetType, targetId, ex);
        }
        throw new AppException(ErrorCode.INVALID_STATUS);
    }

    private void publishReportNotifications(Report report, UUID affectedUserId) {
        String metadata = "{\"reportId\":\"" + report.getId() + "\",\"status\":\"" + report.getStatus() + "\"}";

        eventPublisher.publishEvent(new NotificationEvent(
                this,
                report.getReporter().getId(),
                "Báo cáo đã được cập nhật",
                "Báo cáo của bạn hiện ở trạng thái " + mapStatusLabel(report.getStatus()) + ".",
                NotificationType.system,
                metadata
        ));

        if (affectedUserId != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    affectedUserId,
                    "Nội dung của bạn đã bị xử lý",
                    "Hệ thống đã áp dụng xử lý sau khi một báo cáo được xác nhận vi phạm.",
                    NotificationType.system,
                    metadata
            ));
        }
    }

    private void publishPendingReportNotification(Report report) {
        String metadata = "{\"reportId\":\"" + report.getId() + "\",\"status\":\"" + report.getStatus() + "\"}";
        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        "Có báo cáo mới cần xử lý",
                        "Hệ thống vừa nhận một báo cáo mới cho " + report.getTargetType().toLowerCase() + ".",
                        NotificationType.system,
                        metadata
                )));
    }

    private Report attachEvidenceFiles(Report report, List<MultipartFile> files) {
        List<MultipartFile> normalizedFiles = normalizeFiles(files);
        validateFiles(normalizedFiles);

        if (normalizedFiles.isEmpty()) {
            return report;
        }

        List<String> uploadedUrls = new ArrayList<>();

        try {
            for (int index = 0; index < normalizedFiles.size(); index++) {
                MultipartFile file = normalizedFiles.get(index);
                String fileUrl = storageService.uploadFile(file, buildReportEvidenceFolder(report.getId()));
                uploadedUrls.add(fileUrl);
                report.addEvidenceFile(ReportFile.builder()
                        .fileUrl(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .sortOrder(index)
                        .build());
            }

            return reportRepository.save(report);
        } catch (RuntimeException exception) {
            uploadedUrls.forEach(storageService::deleteFile);
            throw exception;
        }
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_REPORT_EVIDENCE_FILES) {
            throw new AppException(ErrorCode.REPORT_EVIDENCE_LIMIT_EXCEEDED);
        }

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new AppException(ErrorCode.REPORT_EVIDENCE_IMAGE_ONLY);
            }
        }
    }

    private List<ReportEvidenceFileResponseDTO> mapEvidenceFiles(Collection<ReportFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
                .map(file -> ReportEvidenceFileResponseDTO.builder()
                        .id(file.getId())
                        .fileUrl(file.getFileUrl())
                        .fileName(file.getFileName())
                        .contentType(file.getContentType())
                        .sortOrder(file.getSortOrder())
                        .build())
                .toList();
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
                .evidenceFiles(mapEvidenceFiles(report.getEvidenceFiles()))
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .processedById(report.getProcessedBy() != null ? report.getProcessedBy().getId() : null)
                .processedByName(report.getProcessedBy() != null ? report.getProcessedBy().getFullName() : null)
                .createdAt(report.getCreatedAt())
                .processedAt(report.getProcessedAt())
                .build();
    }

    private String buildReportEvidenceFolder(UUID reportId) {
        return "reports/" + reportId;
    }

    private String normalizeTargetType(String targetType) {
        return targetType == null ? null : targetType.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String mapStatusLabel(ReportStatus status) {
        return switch (status) {
            case pending -> "chờ xử lý";
            case investigating -> "đang điều tra";
            case resolved_upheld -> "xác nhận vi phạm";
            case resolved_dismissed -> "bác bỏ";
        };
    }
}
