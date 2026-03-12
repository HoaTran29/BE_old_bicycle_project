package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.WishlistItemResponseDTO;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.Wishlist;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.repository.WishlistRepository;
import com.backend.old_bicycle_project.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public WishlistItemResponseDTO addProduct(UUID userId, UUID productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (product.getStatus() != ProductStatus.active && product.getStatus() != ProductStatus.inspected_passed) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Wishlist wishlist = wishlistRepository.save(Wishlist.builder()
                .user(user)
                .product(product)
                .build());

        return mapToDTO(wishlist);
    }

    @Override
    @Transactional
    public void removeProduct(UUID userId, UUID productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new AppException(ErrorCode.RECORD_NOT_EXISTS);
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponseDTO> getWishlist(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private WishlistItemResponseDTO mapToDTO(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        String primaryImageUrl = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
                        .thenComparingInt(ProductImage::getDisplayOrder))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);

        return WishlistItemResponseDTO.builder()
                .productId(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .primaryImageUrl(primaryImageUrl)
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
