package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface PaymentService {

    PaymentResponseDTO createPaymentUrl(UUID orderId, User currentUser, HttpServletRequest request);

    void processVnPayCallback(HttpServletRequest request);
}
