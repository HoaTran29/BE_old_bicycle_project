package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid OrderCreateRequestDTO requestDTO) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Order created successfully")
                .result(orderService.createOrder(currentUser, requestDTO))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<OrderResponseDTO>> getMyOrders(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<OrderResponseDTO>>builder()
                .message("Fetched orders successfully")
                .result(orderService.getMyOrders(currentUser))
                .build();
    }

    @PatchMapping("/{orderId}/confirm-deposit")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<OrderResponseDTO> confirmDeposit(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Deposit confirmed successfully")
                .result(orderService.confirmDeposit(orderId, currentUser))
                .build();
    }

    @PatchMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<OrderResponseDTO> completeOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Order completed successfully")
                .result(orderService.completeOrder(orderId, currentUser))
                .build();
    }

    @PatchMapping("/{orderId}/accept")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<OrderResponseDTO> acceptOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Order accepted successfully")
                .result(orderService.acceptOrder(orderId, currentUser))
                .build();
    }

    @PatchMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<OrderResponseDTO> shipOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Order shipped successfully")
                .result(orderService.shipOrder(orderId, currentUser))
                .build();
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ApiResponse<OrderResponseDTO> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<OrderResponseDTO>builder()
                .message("Order cancelled successfully")
                .result(orderService.cancelOrder(orderId, currentUser))
                .build();
    }
}
