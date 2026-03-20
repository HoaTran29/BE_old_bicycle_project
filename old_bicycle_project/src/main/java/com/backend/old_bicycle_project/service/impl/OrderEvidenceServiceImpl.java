package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.OrderEvidenceFileResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.OrderEvidenceFile;
import com.backend.old_bicycle_project.entity.OrderEvidenceSubmission;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderEvidenceSubmissionRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;
import com.backend.old_bicycle_project.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEvidenceServiceImpl implements OrderEvidenceService {

    private static final int MAX_EVIDENCE_FILES = 3;

    private final OrderEvidenceSubmissionRepository orderEvidenceSubmissionRepository;
    private final StorageService storageService;

    @Override
    @Transactional
    public OrderEvidenceSubmissionResponseDTO createSellerHandoverEvidence(
            Order order,
            User submittedBy,
            String note,
            List<MultipartFile> files
    ) {
        return createSubmission(order, submittedBy, OrderEvidenceType.seller_handover, note, files, true, false);
    }

    @Override
    @Transactional
    public OrderEvidenceSubmissionResponseDTO createBuyerReceiptEvidence(
            Order order,
            User submittedBy,
            String note,
            List<MultipartFile> files
    ) {
        return createSubmission(order, submittedBy, OrderEvidenceType.buyer_receipt, note, files, false, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> getEvidenceByOrderIds(Collection<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OrderEvidenceSubmission> submissions = orderEvidenceSubmissionRepository.findDetailedByOrderIds(orderIds);
        Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> result = new LinkedHashMap<>();

        for (OrderEvidenceSubmission submission : submissions) {
            result.computeIfAbsent(submission.getOrder().getId(), ignored -> new EnumMap<>(OrderEvidenceType.class))
                    .put(submission.getEvidenceType(), mapToDTO(submission));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO> getEvidenceByOrderId(UUID orderId) {
        if (orderId == null) {
            return Collections.emptyMap();
        }

        return getEvidenceByOrderIds(List.of(orderId)).getOrDefault(orderId, Collections.emptyMap());
    }

    private OrderEvidenceSubmissionResponseDTO createSubmission(
            Order order,
            User submittedBy,
            OrderEvidenceType evidenceType,
            String note,
            List<MultipartFile> files,
            boolean requireFiles,
            boolean skipWhenEmpty
    ) {
        List<MultipartFile> normalizedFiles = normalizeFiles(files);
        String normalizedNote = normalizeNote(note);

        if (skipWhenEmpty && normalizedFiles.isEmpty() && normalizedNote == null) {
            return null;
        }

        validateFiles(normalizedFiles, requireFiles);

        if (orderEvidenceSubmissionRepository.existsByOrderIdAndEvidenceType(order.getId(), evidenceType)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        OrderEvidenceSubmission submission = OrderEvidenceSubmission.builder()
                .order(order)
                .submittedByUser(submittedBy)
                .submittedByRole(submittedBy.getRole())
                .evidenceType(evidenceType)
                .note(normalizedNote)
                .build();

        List<String> uploadedUrls = new ArrayList<>();

        try {
            for (int index = 0; index < normalizedFiles.size(); index++) {
                MultipartFile file = normalizedFiles.get(index);
                String fileUrl = storageService.uploadFile(file, buildFolder(order.getId(), evidenceType));
                uploadedUrls.add(fileUrl);
                submission.addFile(OrderEvidenceFile.builder()
                        .fileUrl(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .sortOrder(index)
                        .build());
            }

            return mapToDTO(orderEvidenceSubmissionRepository.save(submission));
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

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }

        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateFiles(List<MultipartFile> files, boolean requireFiles) {
        if (requireFiles && files.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_EVIDENCE_REQUIRED);
        }

        if (files.size() > MAX_EVIDENCE_FILES) {
            throw new AppException(ErrorCode.ORDER_EVIDENCE_LIMIT_EXCEEDED);
        }

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new AppException(ErrorCode.ORDER_EVIDENCE_IMAGE_ONLY);
            }
        }
    }

    private String buildFolder(UUID orderId, OrderEvidenceType evidenceType) {
        return "orders/" + orderId + "/" + evidenceType.name();
    }

    private OrderEvidenceSubmissionResponseDTO mapToDTO(OrderEvidenceSubmission submission) {
        return OrderEvidenceSubmissionResponseDTO.builder()
                .id(submission.getId())
                .evidenceType(submission.getEvidenceType())
                .submittedByUserId(submission.getSubmittedByUser().getId())
                .submittedByName(submission.getSubmittedByUser().getFullName())
                .submittedByRole(submission.getSubmittedByRole())
                .note(submission.getNote())
                .createdAt(submission.getCreatedAt())
                .files(submission.getFiles().stream()
                        .map(file -> OrderEvidenceFileResponseDTO.builder()
                                .id(file.getId())
                                .fileUrl(file.getFileUrl())
                                .fileName(file.getFileName())
                                .contentType(file.getContentType())
                                .sortOrder(file.getSortOrder())
                                .build())
                        .toList())
                .build();
    }
}
