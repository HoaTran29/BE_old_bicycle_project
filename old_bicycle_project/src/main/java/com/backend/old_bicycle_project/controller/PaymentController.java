package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/create-url")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PaymentResponseDTO> createPaymentUrl(
            @RequestParam UUID orderId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        return ApiResponse.<PaymentResponseDTO>builder()
                .result(paymentService.createPaymentUrl(orderId, currentUser, request))
                .message("Payment URL generated successfully")
                .build();
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        paymentService.processVnPayCallback(request);
        // Redirect to frontend result page
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        if ("00".equals(vnp_ResponseCode)) {
            response.sendRedirect(frontendUrl + "/payment-result?status=success");
        } else {
            response.sendRedirect(frontendUrl + "/payment-result?status=failed");
        }
    }

    @GetMapping("/vnpay-ipn")
    public ApiResponse<String> vnpayIpn(HttpServletRequest request) {
        // IPN usually requires returning a specific JSON/String to VNPay.
        // For simplicity, we process it and return success for now.
        paymentService.processVnPayCallback(request);
        return ApiResponse.<String>builder()
                .result("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}")
                .build();
    }
}
