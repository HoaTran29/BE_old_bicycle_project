package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionHistoryItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionRequestItemResponseDTO;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.ProductImageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.StorageService;
import com.backend.old_bicycle_project.support.TestMultipartFiles;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceImplTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InspectionServiceImpl inspectionService;

    private User seller;
    private User inspector;
    private User admin;
    private Product product;
    private Inspection inspection;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(UUID.randomUUID())
                .firstName("Lan")
                .lastName("Nguyen")
                .phone("0909000001")
                .role(AppRole.seller)
                .build();

        inspector = User.builder()
                .id(UUID.randomUUID())
                .firstName("Khanh")
                .lastName("Pham")
                .role(AppRole.inspector)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("Tran")
                .role(AppRole.admin)
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Giant Propel Advanced")
                .price(BigDecimal.valueOf(52000000))
                .province("Ho Chi Minh")
                .status(ProductStatus.pending_inspection)
                .images(new java.util.ArrayList<>())
                .build();

        product.getImages().add(ProductImage.builder()
                .product(product)
                .url("https://cdn.test/propel.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build());

        inspection = Inspection.builder()
                .id(UUID.randomUUID())
                .product(product)
                .inspector(inspector)
                .overallScore(BigDecimal.valueOf(4.4))
                .passed(true)
                .validUntil(LocalDateTime.of(2026, 3, 25, 10, 0))
                .createdAt(LocalDateTime.of(2026, 3, 18, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 18, 10, 30))
                .build();
    }

    @Test
    void getInspectionRequestsReturnsPendingProducts() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(inspectionRepository.findByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of(inspection));
        when(productImageRepository.findByProductIdInOrderByProductIdAscDisplayOrderAsc(List.of(product.getId())))
                .thenReturn(product.getImages());

        var result = inspectionService.getInspectionRequests("giant", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        InspectionRequestItemResponseDTO item = result.getContent().get(0);
        assertThat(item.getProductId()).isEqualTo(product.getId());
        assertThat(item.getSellerName()).isEqualTo("Lan Nguyen");
        assertThat(item.getProductImageUrl()).isEqualTo("https://cdn.test/propel.jpg");
    }

    @Test
    void getInspectionRequestsFallsBackWhenPendingProductHasNoInspectionRow() {
        product.setUpdatedAt(LocalDateTime.of(2026, 3, 18, 9, 15));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(inspectionRepository.findByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of());
        when(productImageRepository.findByProductIdInOrderByProductIdAscDisplayOrderAsc(List.of(product.getId())))
                .thenReturn(product.getImages());

        var result = inspectionService.getInspectionRequests(null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        InspectionRequestItemResponseDTO item = result.getContent().get(0);
        assertThat(item.getInspectionId()).isEqualTo(product.getId());
        assertThat(item.getRequestedAt()).isEqualTo(LocalDateTime.of(2026, 3, 18, 9, 15));
    }

    @Test
    void getInspectionHistoryReturnsMappedInspectionItems() {
        when(inspectionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inspection)));
        when(productImageRepository.findByProductIdInOrderByProductIdAscDisplayOrderAsc(List.of(product.getId())))
                .thenReturn(product.getImages());

        var result = inspectionService.getInspectionHistory(inspector, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        InspectionHistoryItemResponseDTO item = result.getContent().get(0);
        assertThat(item.getInspectorName()).isEqualTo("Khanh Pham");
        assertThat(item.getOverallScore()).isEqualTo(BigDecimal.valueOf(4.4));
        assertThat(item.getEvaluatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 18, 10, 30));
    }

    @Test
    void getInspectionDashboardBuildsInspectorSummary() {
        when(productRepository.countByStatus(ProductStatus.pending_inspection)).thenReturn(3L);
        when(inspectionRepository.countByInspectorIdAndUpdatedAtAfter(any(UUID.class), any(LocalDateTime.class))).thenReturn(2L);
        when(inspectionRepository.countByInspectorId(inspector.getId())).thenReturn(4L);
        when(inspectionRepository.countByInspectorIdAndPassedTrue(inspector.getId())).thenReturn(3L);
        when(inspectionRepository.findAverageOverallScoreByInspectorId(inspector.getId())).thenReturn(BigDecimal.valueOf(4.4));
        when(inspectionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inspection)));
        when(productImageRepository.findByProductIdInOrderByProductIdAscDisplayOrderAsc(List.of(product.getId())))
                .thenReturn(product.getImages());

        InspectionDashboardResponseDTO result = inspectionService.getInspectionDashboard(inspector);

        assertThat(result.getPendingRequests()).isEqualTo(3L);
        assertThat(result.getCompletedThisWeek()).isEqualTo(2L);
        assertThat(result.getPassRate()).isEqualByComparingTo("75.0");
        assertThat(result.getAverageScore()).isEqualByComparingTo("4.4");
        assertThat(result.getRecentInspections()).hasSize(1);
    }

    @Test
    void getInspectionByProductIdReturnsNullWhenProductHasNoInspectionYet() {
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(java.util.Optional.empty());

        var result = inspectionService.getInspectionByProductId(product.getId());

        assertThat(result).isNull();
    }

    @Test
    void requestInspectionAllowsAdminToRoutePendingProductIntoInspectionQueue() {
        product.setStatus(ProductStatus.pending);

        when(productRepository.findById(product.getId())).thenReturn(java.util.Optional.of(product));
        when(userRepository.findById(admin.getId())).thenReturn(java.util.Optional.of(admin));
        when(userRepository.findByRole(AppRole.inspector)).thenReturn(List.of(inspector));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(java.util.Optional.empty());
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> {
            Inspection savedInspection = invocation.getArgument(0);
            if (savedInspection.getId() == null) {
                savedInspection.setId(UUID.randomUUID());
            }
            return savedInspection;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inspectionService.requestInspection(product.getId(), admin.getId());

        assertThat(result.getProductId()).isEqualTo(product.getId());
        assertThat(product.getStatus()).isEqualTo(ProductStatus.pending_inspection);
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(NotificationEvent::getUserId)
                .containsExactlyInAnyOrder(seller.getId(), inspector.getId());
        assertThat(eventCaptor.getAllValues()).extracting(NotificationEvent::getType)
                .containsOnly(NotificationType.inspection);
    }

    @Test
    void evaluateInspectionCreatesMissingInspectionRowForPendingRequest() {
        InspectionEvaluationDTO dto = InspectionEvaluationDTO.builder()
                .frameScore(4)
                .forkScore(4)
                .brakesScore(5)
                .drivetrainScore(4)
                .wheelsScore(5)
                .wearPercentage(20)
                .expertNotes("Có thể giao dịch bình thường.")
                .passed(true)
                .build();

        product.setUpdatedAt(LocalDateTime.of(2026, 3, 18, 9, 30));

        when(productRepository.findById(product.getId())).thenReturn(java.util.Optional.of(product));
        when(userRepository.findById(inspector.getId())).thenReturn(java.util.Optional.of(inspector));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(java.util.Optional.empty());
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> {
            Inspection savedInspection = invocation.getArgument(0);
            if (savedInspection.getId() == null) {
                savedInspection.setId(UUID.randomUUID());
            }
            return savedInspection;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inspectionService.evaluateInspection(product.getId(), inspector.getId(), dto);

        assertThat(result.getInspectorId()).isEqualTo(inspector.getId());
        assertThat(result.getPassed()).isTrue();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.active);
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(seller.getId());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(NotificationType.inspection);
    }

    @Test
    void uploadInspectionReportStoresReportUrlOnExistingInspection() {
        MockMultipartFile reportFile = TestMultipartFiles.pdf("reportFile", "inspection-report.pdf");
        inspection.setReportFileUrl("https://cdn.test/old-report.pdf");

        when(productRepository.findById(product.getId())).thenReturn(java.util.Optional.of(product));
        when(userRepository.findById(inspector.getId())).thenReturn(java.util.Optional.of(inspector));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(java.util.Optional.of(inspection));
        when(storageService.uploadFile(reportFile, "inspections/" + product.getId()))
                .thenReturn("https://cdn.test/new-report.pdf");
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inspectionService.uploadInspectionReport(product.getId(), inspector.getId(), reportFile);

        assertThat(result.getReportFileUrl()).isEqualTo("https://cdn.test/new-report.pdf");
        verify(storageService).deleteFile("https://cdn.test/old-report.pdf");
    }

    @Test
    void uploadInspectionReportRejectsNonPdfFile() {
        MockMultipartFile reportFile = TestMultipartFiles.text("reportFile", "inspection-report.txt");

        assertThatThrownBy(() -> inspectionService.uploadInspectionReport(product.getId(), inspector.getId(), reportFile))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSPECTION_REPORT_INVALID);
    }
}
