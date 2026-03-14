package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.WishlistItemResponseDTO;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.Wishlist;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Test
    void addProductReturnsMappedWishlistItemWithPrimaryImage() {
        User buyer = user(AppRole.buyer, "buyer@test.dev", "Ngọc", "Buyer");
        User seller = user(AppRole.seller, "seller@test.dev", "An", "Seller");
        Product product = product(seller, ProductStatus.active);
        Wishlist wishlist = Wishlist.builder()
                .user(buyer)
                .product(product)
                .createdAt(LocalDateTime.of(2026, 3, 13, 9, 30))
                .build();

        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserIdAndProductId(buyer.getId(), product.getId())).thenReturn(false);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);

        WishlistItemResponseDTO response = wishlistService.addProduct(buyer.getId(), product.getId());

        assertThat(response.getProductId()).isEqualTo(product.getId());
        assertThat(response.getSellerId()).isEqualTo(seller.getId());
        assertThat(response.getSellerName()).isEqualTo("An Seller");
        assertThat(response.getPrimaryImageUrl()).isEqualTo("https://cdn.example/primary.jpg");
        assertThat(response.getAddedAt()).isEqualTo(LocalDateTime.of(2026, 3, 13, 9, 30));
    }

    @Test
    void addProductRejectsSellerAddingOwnListing() {
        User seller = user(AppRole.seller, "seller@test.dev", "An", "Seller");
        Product product = product(seller, ProductStatus.active);

        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> wishlistService.addProduct(seller.getId(), product.getId()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getWishlistRejectsUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> wishlistService.getWishlist(userId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void removeProductDeletesExistingWishlistEntry() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(wishlistRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        wishlistService.removeProduct(userId, productId);

        verify(wishlistRepository).deleteByUserIdAndProductId(userId, productId);
    }

    private Product product(User seller, ProductStatus status) {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Trek Domane")
                .price(new BigDecimal("12500000"))
                .status(status)
                .images(List.of(
                        ProductImage.builder()
                                .url("https://cdn.example/secondary.jpg")
                                .isPrimary(false)
                                .displayOrder(1)
                                .build(),
                        ProductImage.builder()
                                .url("https://cdn.example/primary.jpg")
                                .isPrimary(true)
                                .displayOrder(0)
                                .build()
                ))
                .build();
        product.getImages().forEach(image -> image.setProduct(product));
        return product;
    }

    private User user(AppRole role, String email, String firstName, String lastName) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .build();
    }
}
