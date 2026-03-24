package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.AdminBrandUpsertRequest;
import com.backend.old_bicycle_project.dto.request.AdminCategoryUpsertRequest;
import com.backend.old_bicycle_project.dto.request.AdminReferenceValueUpsertRequest;
import com.backend.old_bicycle_project.dto.request.SizeChartRowRequestDTO;
import com.backend.old_bicycle_project.dto.request.SizeChartUpsertRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.BrandResponseDTO;
import com.backend.old_bicycle_project.dto.response.CategoryResponseDTO;
import com.backend.old_bicycle_project.dto.response.ReferenceValueResponseDTO;
import com.backend.old_bicycle_project.dto.response.SizeChartResponseDTO;
import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.entity.Groupset;
import com.backend.old_bicycle_project.entity.SizeChart;
import com.backend.old_bicycle_project.entity.SizeChartRow;
import com.backend.old_bicycle_project.service.BrandService;
import com.backend.old_bicycle_project.service.BrakeTypeService;
import com.backend.old_bicycle_project.service.CategoryService;
import com.backend.old_bicycle_project.service.FrameMaterialService;
import com.backend.old_bicycle_project.service.GroupsetService;
import com.backend.old_bicycle_project.service.SizeChartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataControllerTest {

    @Mock
    private BrandService brandService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BrakeTypeService brakeTypeService;

    @Mock
    private FrameMaterialService frameMaterialService;

    @Mock
    private GroupsetService groupsetService;

    @Mock
    private SizeChartService sizeChartService;

    @InjectMocks
    private ReferenceDataController referenceDataController;

    @Test
    void getAllBrandsMapsEntityToDto() {
        Brand brand = Brand.builder()
                .id(UUID.randomUUID())
                .name("Trek")
                .logoUrl("https://cdn.test/trek.svg")
                .build();
        when(brandService.getAll()).thenReturn(List.of(brand));

        ApiResponse<List<BrandResponseDTO>> response = referenceDataController.getAllBrands();

        assertThat(response.getResult()).hasSize(1);
        assertThat(response.getResult().getFirst().getName()).isEqualTo("Trek");
    }

    @Test
    void updateCategoryDelegatesAndWrapsDto() {
        UUID categoryId = UUID.randomUUID();
        AdminCategoryUpsertRequest request = new AdminCategoryUpsertRequest();
        request.setName("Road Bike");
        request.setSlug("road-bike");
        Category category = Category.builder()
                .id(categoryId)
                .name("Road Bike")
                .slug("road-bike")
                .build();
        when(categoryService.update(categoryId, "Road Bike", "road-bike", null)).thenReturn(category);

        ApiResponse<CategoryResponseDTO> response = referenceDataController.updateCategory(categoryId, request);

        assertThat(response.getResult().getSlug()).isEqualTo("road-bike");
        verify(categoryService).update(categoryId, "Road Bike", "road-bike", null);
    }

    @Test
    void createBrandDelegatesToBrandService() {
        AdminBrandUpsertRequest request = new AdminBrandUpsertRequest();
        request.setName("Giant");
        request.setLogoUrl("https://cdn.test/giant.svg");
        Brand brand = Brand.builder().id(UUID.randomUUID()).name("Giant").logoUrl(request.getLogoUrl()).build();
        when(brandService.create("Giant", "https://cdn.test/giant.svg")).thenReturn(brand);

        ApiResponse<BrandResponseDTO> response = referenceDataController.createBrand(request);

        assertThat(response.getResult().getName()).isEqualTo("Giant");
        verify(brandService).create("Giant", "https://cdn.test/giant.svg");
    }

    @Test
    void createBrakeTypeDelegatesToService() {
        AdminReferenceValueUpsertRequest request = new AdminReferenceValueUpsertRequest();
        request.setName("Disc");
        request.setDescription("Hydraulic disc brake");
        BrakeType brakeType = BrakeType.builder()
                .id(UUID.randomUUID())
                .name("Disc")
                .description("Hydraulic disc brake")
                .build();
        when(brakeTypeService.create("Disc", "Hydraulic disc brake")).thenReturn(brakeType);

        ApiResponse<ReferenceValueResponseDTO> response = referenceDataController.createBrakeType(request);

        assertThat(response.getResult().getDescription()).isEqualTo("Hydraulic disc brake");
        verify(brakeTypeService).create("Disc", "Hydraulic disc brake");
    }

    @Test
    void createFrameMaterialDelegatesToService() {
        AdminReferenceValueUpsertRequest request = new AdminReferenceValueUpsertRequest();
        request.setName("Carbon");
        request.setDescription("Carbon frame");
        FrameMaterial frameMaterial = FrameMaterial.builder()
                .id(UUID.randomUUID())
                .name("Carbon")
                .description("Carbon frame")
                .build();
        when(frameMaterialService.create("Carbon", "Carbon frame")).thenReturn(frameMaterial);

        ApiResponse<ReferenceValueResponseDTO> response = referenceDataController.createFrameMaterial(request);

        assertThat(response.getResult().getName()).isEqualTo("Carbon");
        verify(frameMaterialService).create("Carbon", "Carbon frame");
    }

    @Test
    void getAllGroupsetsMapsEntityToDto() {
        Groupset groupset = Groupset.builder()
                .id(UUID.randomUUID())
                .name("Shimano 105")
                .description("11-speed road groupset")
                .build();
        when(groupsetService.getAll()).thenReturn(List.of(groupset));

        ApiResponse<List<ReferenceValueResponseDTO>> response = referenceDataController.getAllGroupsets();

        assertThat(response.getResult()).hasSize(1);
        assertThat(response.getResult().getFirst().getName()).isEqualTo("Shimano 105");
    }

    @Test
    void createGroupsetDelegatesToService() {
        AdminReferenceValueUpsertRequest request = new AdminReferenceValueUpsertRequest();
        request.setName("SRAM Rival");
        request.setDescription("12-speed road groupset");
        Groupset groupset = Groupset.builder()
                .id(UUID.randomUUID())
                .name("SRAM Rival")
                .description("12-speed road groupset")
                .build();
        when(groupsetService.create("SRAM Rival", "12-speed road groupset")).thenReturn(groupset);

        ApiResponse<ReferenceValueResponseDTO> response = referenceDataController.createGroupset(request);

        assertThat(response.getResult().getName()).isEqualTo("SRAM Rival");
        verify(groupsetService).create("SRAM Rival", "12-speed road groupset");
    }

    @Test
    void getSizeChartByCategoryMapsRowsAndCategoryMetadata() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Road Bike")
                .slug("road-bike")
                .build();
        SizeChart sizeChart = SizeChart.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Road bike size guide")
                .description("Height guidance for road bikes")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .rows(List.of(
                        SizeChartRow.builder()
                                .id(UUID.randomUUID())
                                .frameSize("54")
                                .heightMinCm(170)
                                .heightMaxCm(178)
                                .note("Tư thế road tiêu chuẩn")
                                .displayOrder(0)
                                .build()
                ))
                .build();
        when(sizeChartService.getByCategory(categoryId)).thenReturn(Optional.of(sizeChart));

        ApiResponse<SizeChartResponseDTO> response = referenceDataController.getSizeChartByCategory(categoryId);

        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getCategoryName()).isEqualTo("Road Bike");
        assertThat(response.getResult().getRows()).hasSize(1);
        assertThat(response.getResult().getRows().getFirst().getFrameSize()).isEqualTo("54");
    }

    @Test
    void createSizeChartDelegatesToService() {
        UUID categoryId = UUID.randomUUID();
        SizeChartUpsertRequestDTO request = new SizeChartUpsertRequestDTO();
        request.setCategoryId(categoryId);
        request.setName("Road bike size guide");
        request.setDescription("Height guidance");

        SizeChartRowRequestDTO rowRequest = new SizeChartRowRequestDTO();
        rowRequest.setFrameSize("54");
        rowRequest.setHeightMinCm(170);
        rowRequest.setHeightMaxCm(178);
        rowRequest.setNote("Road fit");
        request.setRows(List.of(rowRequest));

        Category category = Category.builder()
                .id(categoryId)
                .name("Road Bike")
                .slug("road-bike")
                .build();
        SizeChart sizeChart = SizeChart.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Road bike size guide")
                .description("Height guidance")
                .rows(List.of(
                        SizeChartRow.builder()
                                .id(UUID.randomUUID())
                                .frameSize("54")
                                .heightMinCm(170)
                                .heightMaxCm(178)
                                .note("Road fit")
                                .displayOrder(0)
                                .build()
                ))
                .build();
        when(sizeChartService.create(request)).thenReturn(sizeChart);

        ApiResponse<SizeChartResponseDTO> response = referenceDataController.createSizeChart(request);

        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getName()).isEqualTo("Road bike size guide");
        verify(sizeChartService).create(request);
    }
}
