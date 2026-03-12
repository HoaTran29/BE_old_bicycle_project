package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.WishlistItemResponseDTO;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    WishlistItemResponseDTO addProduct(UUID userId, UUID productId);

    void removeProduct(UUID userId, UUID productId);

    List<WishlistItemResponseDTO> getWishlist(UUID userId);
}
