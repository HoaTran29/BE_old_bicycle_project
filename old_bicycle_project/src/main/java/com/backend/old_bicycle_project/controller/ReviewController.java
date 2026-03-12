package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.ReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ReviewResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitReview(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid ReviewRequestDTO requestDTO) {
        ReviewResponseDTO responseDTO = reviewService.submitReview(orderId, currentUser.getId(), requestDTO);
        return ResponseEntity.ok(ApiResponse.<ReviewResponseDTO>builder()
                .code(200)
                .message("Review submitted successfully")
                .result(responseDTO)
                .build());
    }

    @GetMapping("/api/users/{sellerId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getSellerReviews(
            @PathVariable UUID sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponseDTO> reviews = reviewService.getSellerReviews(sellerId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ReviewResponseDTO>>builder()
                .code(200)
                .message("Fetched reviews successfully")
                .result(reviews)
                .build());
    }
}
