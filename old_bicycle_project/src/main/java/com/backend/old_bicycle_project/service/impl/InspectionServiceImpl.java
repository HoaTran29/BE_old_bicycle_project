package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionHistoryItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionRequestItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.ProductImageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.InspectionService;
import com.backend.old_bicycle_project.specification.InspectionSpecification;
import com.backend.old_bicycle_project.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository inspectionRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
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

        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (product.getStatus() != ProductStatus.pending_inspection) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        Inspection inspection = inspectionRepository.findByProductId(productId)
                .orElseGet(() -> Inspection.builder()
                        .product(product)
                        .createdAt(product.getUpdatedAt() != null ? product.getUpdatedAt() : LocalDateTime.now())
                        .build());

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
        return inspectionRepository.findByProductId(productId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionRequestItemResponseDTO> getInspectionRequests(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Product> requestPage = productRepository.findAll(ProductSpecification.fromInspectionRequestFilter(keyword), pageable);

        if (requestPage.isEmpty()) {
            return requestPage.map(product -> mapRequestItem(product, null, null));
        }

        Map<UUID, Inspection> inspectionsByProductId = inspectionRepository.findByProductIdIn(
                        requestPage.getContent().stream().map(Product::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        inspection -> inspection.getProduct().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        Map<UUID, String> primaryImageUrlsByProductId = resolvePrimaryImageUrls(
                requestPage.getContent().stream().map(Product::getId).toList()
        );

        return requestPage.map(product -> mapRequestItem(
                product,
                inspectionsByProductId.get(product.getId()),
                primaryImageUrlsByProductId.get(product.getId())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionHistoryItemResponseDTO> getInspectionHistory(User currentUser, String keyword, int page, int size) {
        UUID inspectorFilter = isAdmin(currentUser) ? null : currentUser.getId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Inspection> historyPage = inspectionRepository.findAll(InspectionSpecification.fromHistoryFilter(inspectorFilter, keyword), pageable);

        if (historyPage.isEmpty()) {
            return historyPage.map(inspection -> mapHistoryItem(inspection, null));
        }

        Map<UUID, String> primaryImageUrlsByProductId = resolvePrimaryImageUrls(
                historyPage.getContent().stream()
                        .map(inspection -> inspection.getProduct().getId())
                        .toList()
        );

        return historyPage.map(inspection -> mapHistoryItem(
                inspection,
                primaryImageUrlsByProductId.get(inspection.getProduct().getId())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionDashboardResponseDTO getInspectionDashboard(User currentUser) {
        boolean admin = isAdmin(currentUser);
        UUID inspectorId = admin ? null : currentUser.getId();
        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(java.time.DayOfWeek.MONDAY)
                .with(LocalTime.MIN);

        long pendingRequests = productRepository.countByStatus(ProductStatus.pending_inspection);
        long completedThisWeek = admin
                ? inspectionRepository.countByInspectorIsNotNullAndUpdatedAtAfter(startOfWeek)
                : inspectionRepository.countByInspectorIdAndUpdatedAtAfter(inspectorId, startOfWeek);
        long totalCompleted = admin
                ? inspectionRepository.countByInspectorIsNotNull()
                : inspectionRepository.countByInspectorId(inspectorId);
        long passedCount = admin
                ? inspectionRepository.countByInspectorIsNotNullAndPassedTrue()
                : inspectionRepository.countByInspectorIdAndPassedTrue(inspectorId);

        BigDecimal passRate = totalCompleted == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passedCount * 100.0 / totalCompleted).setScale(1, RoundingMode.HALF_UP);
        BigDecimal averageScore = admin
                ? inspectionRepository.findAverageOverallScoreForAll()
                : inspectionRepository.findAverageOverallScoreByInspectorId(inspectorId);

        Page<InspectionHistoryItemResponseDTO> recentInspections = getInspectionHistory(currentUser, null, 0, 5);

        return InspectionDashboardResponseDTO.builder()
                .pendingRequests(pendingRequests)
                .completedThisWeek(completedThisWeek)
                .passRate(passRate)
                .averageScore(averageScore != null ? averageScore.setScale(1, RoundingMode.HALF_UP) : null)
                .recentInspections(recentInspections.getContent())
                .build();
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
                .updatedAt(inspection.getUpdatedAt())
                .build();
    }

    private InspectionRequestItemResponseDTO mapRequestItem(Product product, Inspection inspection, String productImageUrl) {
        return InspectionRequestItemResponseDTO.builder()
                .inspectionId(inspection != null ? inspection.getId() : product.getId())
                .productId(product.getId())
                .productTitle(product.getTitle())
                .productPrice(product.getPrice())
                .province(product.getProvince())
                .productImageUrl(productImageUrl)
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .sellerPhone(product.getSeller().getPhone())
                .requestedAt(inspection != null
                        ? inspection.getCreatedAt()
                        : (product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt()))
                .build();
    }

    private InspectionHistoryItemResponseDTO mapHistoryItem(Inspection inspection, String productImageUrl) {
        Product product = inspection.getProduct();
        User seller = product.getSeller();
        User inspector = inspection.getInspector();

        return InspectionHistoryItemResponseDTO.builder()
                .inspectionId(inspection.getId())
                .productId(product.getId())
                .productTitle(product.getTitle())
                .productPrice(product.getPrice())
                .province(product.getProvince())
                .productImageUrl(productImageUrl)
                .sellerId(seller.getId())
                .sellerName(seller.getFullName())
                .sellerPhone(seller.getPhone())
                .inspectorId(inspector != null ? inspector.getId() : null)
                .inspectorName(inspector != null ? inspector.getFullName() : null)
                .overallScore(inspection.getOverallScore())
                .passed(inspection.getPassed())
                .requestedAt(inspection.getCreatedAt())
                .evaluatedAt(inspection.getUpdatedAt())
                .validUntil(inspection.getValidUntil())
                .build();
    }

    private Map<UUID, String> resolvePrimaryImageUrls(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productImageRepository.findByProductIdInOrderByProductIdAscDisplayOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(
                        image -> image.getProduct().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::resolvePrimaryImageUrlFromList)
                ));
    }

    private String resolvePrimaryImageUrlFromList(List<ProductImage> images) {
        if (images.isEmpty()) {
            return null;
        }

        return images.stream()
                .sorted((left, right) -> {
                    if (left.isPrimary() == right.isPrimary()) {
                        return Integer.compare(left.getDisplayOrder(), right.getDisplayOrder());
                    }
                    return Boolean.compare(right.isPrimary(), left.isPrimary());
                })
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    private boolean isAdmin(User currentUser) {
        return currentUser.getRole() == AppRole.admin;
    }
}
