package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private AdminProductController adminProductController;

    @Test
    void getAdminProductsDelegatesFiltersToService() {
        UUID sellerId = UUID.randomUUID();
        Page<ProductResponse> page = new PageImpl<>(List.of(ProductResponse.builder()
                .id(UUID.randomUUID())
                .status(ProductStatus.pending)
                .build()));
        when(productService.getAllForAdmin(ProductStatus.pending, sellerId, "trek", 0, 12)).thenReturn(page);

        ApiResponse<Page<ProductResponse>> response = adminProductController.getAdminProducts(
                ProductStatus.pending, sellerId, "trek", 0, 12
        );

        assertThat(response.getResult()).isSameAs(page);
        verify(productService).getAllForAdmin(ProductStatus.pending, sellerId, "trek", 0, 12);
    }

    @Test
    void approveAdminProductDelegatesToActiveStatusTransition() {
        UUID productId = UUID.randomUUID();
        ProductResponse product = ProductResponse.builder()
                .id(productId)
                .status(ProductStatus.active)
                .build();
        when(productService.changeStatus(productId, ProductStatus.active)).thenReturn(product);

        ApiResponse<ProductResponse> response = adminProductController.approveAdminProduct(productId);

        assertThat(response.getResult()).isSameAs(product);
        verify(productService).changeStatus(productId, ProductStatus.active);
    }

    @Test
    void getAdminProductByIdDelegatesToAdminScopedDetailLookup() {
        UUID productId = UUID.randomUUID();
        ProductResponse product = ProductResponse.builder()
                .id(productId)
                .status(ProductStatus.pending)
                .build();
        when(productService.getAdminById(productId)).thenReturn(product);

        ApiResponse<ProductResponse> response = adminProductController.getAdminProductById(productId);

        assertThat(response.getResult()).isSameAs(product);
        verify(productService).getAdminById(productId);
    }

    @Test
    void hideAdminProductDelegatesToHiddenStatusTransition() {
        UUID productId = UUID.randomUUID();
        ProductResponse product = ProductResponse.builder()
                .id(productId)
                .status(ProductStatus.hidden)
                .build();
        when(productService.changeStatus(productId, ProductStatus.hidden)).thenReturn(product);

        ApiResponse<ProductResponse> response = adminProductController.hideAdminProduct(productId);

        assertThat(response.getResult()).isSameAs(product);
        verify(productService).changeStatus(productId, ProductStatus.hidden);
    }
}
