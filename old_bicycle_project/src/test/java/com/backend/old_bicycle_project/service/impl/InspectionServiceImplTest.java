package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionHistoryItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionRequestItemResponseDTO;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceImplTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InspectionServiceImpl inspectionService;

    private User seller;
    private User inspector;
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

        product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Giant Propel Advanced")
                .price(BigDecimal.valueOf(52000000))
                .province("Ho Chi Minh")
                .status(ProductStatus.pending_inspection)
                .images(List.of(ProductImage.builder()
                        .url("https://cdn.test/propel.jpg")
                        .isPrimary(true)
                        .displayOrder(0)
                        .build()))
                .build();

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
        when(inspectionRepository.findByProductId(product.getId()))
                .thenReturn(java.util.Optional.of(inspection));

        var result = inspectionService.getInspectionRequests("giant", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        InspectionRequestItemResponseDTO item = result.getContent().get(0);
        assertThat(item.getProductId()).isEqualTo(product.getId());
        assertThat(item.getSellerName()).isEqualTo("Lan Nguyen");
        assertThat(item.getProductImageUrl()).isEqualTo("https://cdn.test/propel.jpg");
    }

    @Test
    void getInspectionHistoryReturnsMappedInspectionItems() {
        when(inspectionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inspection)));

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
}
