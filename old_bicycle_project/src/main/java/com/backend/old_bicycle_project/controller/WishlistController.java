package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.WishlistItemResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<WishlistItemResponseDTO>> getWishlist(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<WishlistItemResponseDTO>>builder()
                .message("Fetched wishlist successfully")
                .result(wishlistService.getWishlist(currentUser.getId()))
                .build();
    }

    @PostMapping("/{productId}")
    public ApiResponse<WishlistItemResponseDTO> addProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<WishlistItemResponseDTO>builder()
                .message("Product added to wishlist")
                .result(wishlistService.addProduct(currentUser.getId(), productId))
                .build();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal User currentUser) {
        wishlistService.removeProduct(currentUser.getId(), productId);
        return ApiResponse.<Void>builder()
                .message("Product removed from wishlist")
                .build();
    }
}
