package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.product.ProductCreateRequest;
import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.product.ProductUpdateRequest;
import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.BrandRepository;
import com.backend.old_bicycle_project.repository.BrakeTypeRepository;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.FrameMaterialRepository;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductImageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrakeTypeRepository brakeTypeRepository;

    @Mock
    private FrameMaterialRepository frameMaterialRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createRejectsWhenFewerThanThreeImagesAreProvided() {
        ProductCreateRequest request = validCreateRequest();

        assertThatThrownBy(() -> productService.create(request, List.of(image("bike-1.jpg"), image("bike-2.jpg")), seller()))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_MINIMUM_IMAGES_REQUIRED);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createSetsExpiresAtAndUploadsImages() {
        ProductCreateRequest request = validCreateRequest();
        User seller = seller();

        when(brakeTypeRepository.findById(request.getBrakeTypeId())).thenReturn(Optional.of(BrakeType.builder()
                .id(request.getBrakeTypeId())
                .name("Disc")
                .build()));
        when(frameMaterialRepository.findById(request.getFrameMaterialId())).thenReturn(Optional.of(FrameMaterial.builder()
                .id(request.getFrameMaterialId())
                .name("Aluminum")
                .build()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getId() == null) {
                product.setId(UUID.randomUUID());
            }
            if (product.getCreatedAt() == null) {
                product.setCreatedAt(LocalDateTime.of(2026, 3, 13, 10, 0));
            }
            return product;
        });
        when(storageService.uploadFile(any(), anyString()))
                .thenReturn("https://cdn.test/bike-1.jpg", "https://cdn.test/bike-2.jpg", "https://cdn.test/bike-3.jpg");
        when(productImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inspectionRepository.findByProductId(any(UUID.class))).thenReturn(Optional.empty());

        ProductResponse response = productService.create(
                request,
                List.of(image("bike-1.jpg"), image("bike-2.jpg"), image("bike-3.jpg")),
                seller
        );

        assertThat(response.getStatus()).isEqualTo(ProductStatus.pending);
        assertThat(response.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 4, 12, 10, 0));
        assertThat(response.getImages()).hasSize(3);
        verify(productImageRepository).saveAll(anyList());
    }

    @Test
    void getMyProductsUsesSellerScopedRepository() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);

        when(productRepository.findBySellerIdAndDeletedAtIsNull(eq(seller.getId()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        var page = productService.getMyProducts(seller, 0, 12);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(product.getId());
    }

    @Test
    void getMineByIdReturnsOwnedPendingProductForSellerEditFlow() {
        User seller = seller();
        Product product = product(seller, ProductStatus.pending);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        ProductResponse response = productService.getMineById(product.getId(), seller);

        assertThat(response.getId()).isEqualTo(product.getId());
        assertThat(response.getStatus()).isEqualTo(ProductStatus.pending);
    }

    @Test
    void hideMovesOwnedProductToHiddenWithoutSoftDeleting() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        ProductResponse response = productService.hide(product.getId(), seller);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.hidden);
        assertThat(product.getDeletedAt()).isNull();
        verify(productRepository).save(product);
    }

    @Test
    void showMovesHiddenProductBackToPendingAndRenewsExpiry() {
        User seller = seller();
        Product product = product(seller, ProductStatus.hidden);
        LocalDateTime previousExpiry = LocalDateTime.now().minusDays(1);
        product.setExpiresAt(previousExpiry);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        ProductResponse response = productService.show(product.getId(), seller);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.pending);
        assertThat(product.getExpiresAt()).isAfter(previousExpiry);
        verify(productRepository).save(product);
    }

    @Test
    void showRejectsProductThatIsNotHidden() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.show(product.getId(), seller))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteSoftDeletesProductInsteadOfHardDeleting() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);
        product.getImages().add(ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .url("https://cdn.test/bike-1.jpg")
                .displayOrder(0)
                .isPrimary(true)
                .build());
        product.getImages().add(ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .url("https://cdn.test/bike-2.jpg")
                .displayOrder(1)
                .isPrimary(false)
                .build());

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.delete(product.getId(), seller);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getDeletedAt()).isNotNull();
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.hidden);
        assertThat(savedProduct.getImages()).isEmpty();
        verify(storageService).deleteFile("https://cdn.test/bike-1.jpg");
        verify(storageService).deleteFile("https://cdn.test/bike-2.jpg");
        verify(productImageRepository).deleteAllByProductId(product.getId());
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getByIdMarksProductAsVerifiedWhenInspectionIsStillValid() {
        User seller = seller();
        Product product = product(seller, ProductStatus.inspected_passed);
        Inspection inspection = Inspection.builder()
                .id(UUID.randomUUID())
                .product(product)
                .passed(true)
                .overallScore(new BigDecimal("8.8"))
                .reportFileUrl("https://cdn.test/report.pdf")
                .validUntil(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.of(inspection));

        ProductResponse response = productService.getById(product.getId());

        assertThat(response.isVerified()).isTrue();
        assertThat(response.getInspection()).isNotNull();
        assertThat(response.getInspection().getReportFileUrl()).isEqualTo("https://cdn.test/report.pdf");
    }

    @Test
    void getByIdMarksProductAsLockedWhenThereIsAnActiveTransaction() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());
        when(orderRepository.existsByProductIdAndStatusIn(eq(product.getId()), anyList())).thenReturn(true);

        ProductResponse response = productService.getById(product.getId());

        assertThat(response.isLockedForTransaction()).isTrue();
    }

    @Test
    void updateResetsStatusAndInvalidatesExistingInspection() {
        User seller = seller();
        Product product = product(seller, ProductStatus.inspected_passed);
        Inspection inspection = Inspection.builder()
                .id(UUID.randomUUID())
                .product(product)
                .passed(true)
                .validUntil(LocalDateTime.now().plusDays(5))
                .build();

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Xe đã thay groupset");

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(product.getId(), request, null, seller);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.pending);
        assertThat(inspection.getPassed()).isFalse();
        assertThat(inspection.getValidUntil()).isNotNull();
    }

    @Test
    void changeStatusRejectsProductsOutsideAdminModerationStatuses() {
        User seller = seller();
        Product product = product(seller, ProductStatus.sold);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.changeStatus(product.getId(), ProductStatus.active))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateRejectsWhenProductHasActiveTransaction() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Khong duoc sua khi dang co giao dich");

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.existsByProductIdAndStatusIn(eq(product.getId()), anyList())).thenReturn(true);

        assertThatThrownBy(() -> productService.update(product.getId(), request, null, seller))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteRejectsWhenProductHasActiveTransaction() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.existsByProductIdAndStatusIn(eq(product.getId()), anyList())).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(product.getId(), seller))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS);

        verify(productRepository, never()).save(any(Product.class));
        verify(storageService, never()).deleteFile(anyString());
    }

    @Test
    void updateReplacesImageCollectionWithoutBreakingManagedCollectionReference() {
        User seller = seller();
        Product product = product(seller, ProductStatus.active);
        product.getImages().add(ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .url("https://cdn.test/old-bike-1.jpg")
                .displayOrder(0)
                .isPrimary(true)
                .build());
        product.getImages().add(ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .url("https://cdn.test/old-bike-2.jpg")
                .displayOrder(1)
                .isPrimary(false)
                .build());
        product.getImages().add(ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .url("https://cdn.test/old-bike-3.jpg")
                .displayOrder(2)
                .isPrimary(false)
                .build());

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Xe đã cập nhật ảnh");
        request.setFrameSize("L");
        request.setWheelSize("29");

        List<MockMultipartFile> newImages = List.of(
                image("new-bike-1.jpg"),
                image("new-bike-2.jpg"),
                image("new-bike-3.jpg")
        );

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.uploadFile(any(), anyString()))
                .thenReturn("https://cdn.test/new-bike-1.jpg", "https://cdn.test/new-bike-2.jpg", "https://cdn.test/new-bike-3.jpg");
        when(inspectionRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        ProductResponse response = productService.update(product.getId(), request, List.copyOf(newImages), seller);

        assertThat(response.getImages()).hasSize(3);
        assertThat(response.getImages().getFirst().getUrl()).isEqualTo("https://cdn.test/new-bike-1.jpg");
        assertThat(product.getImages()).hasSize(3);
        verify(storageService).deleteFile("https://cdn.test/old-bike-1.jpg");
        verify(storageService).deleteFile("https://cdn.test/old-bike-2.jpg");
        verify(storageService).deleteFile("https://cdn.test/old-bike-3.jpg");
        verify(productImageRepository, atLeastOnce()).deleteAllByProductId(product.getId());
    }

    private ProductCreateRequest validCreateRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("Trek Domane");
        request.setDescription("Xe đường trường đã qua sử dụng");
        request.setPrice(new BigDecimal("18500000"));
        request.setBrakeTypeId(UUID.randomUUID());
        request.setFrameMaterialId(UUID.randomUUID());
        request.setFrameSize("M");
        request.setWheelSize("700c");
        request.setProvince("Hồ Chí Minh");
        return request;
    }

    private Product product(User seller, ProductStatus status) {
        return Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Specialized Allez")
                .price(new BigDecimal("12000000"))
                .frameSize("M")
                .wheelSize("700c")
                .province("Hồ Chí Minh")
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .build();
    }

    private User seller() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("seller@test.dev")
                .firstName("Ngọc")
                .lastName("Seller")
                .role(AppRole.seller)
                .build();
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("images", filename, "image/jpeg", "fake-image".getBytes());
    }
}
