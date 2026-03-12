package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.SepayWebhookRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.PaymentRequestResponseDTO;
import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}/request")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PaymentRequestResponseDTO> createPaymentRequest(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<PaymentRequestResponseDTO>builder()
                .message("Payment request created successfully")
                .result(paymentService.createUpfrontPaymentRequest(orderId, currentUser))
                .build();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<List<PaymentResponseDTO>> getOrderPayments(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<PaymentResponseDTO>>builder()
                .message("Fetched payment history successfully")
                .result(paymentService.getOrderPayments(orderId, currentUser))
                .build();
    }

    @PostMapping("/sepay/webhook")
    public ResponseEntity<ApiResponse<Void>> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody SepayWebhookRequestDTO requestDTO) {
        paymentService.handleSepayWebhook(requestDTO, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Webhook processed successfully")
                .build());
    }
}
