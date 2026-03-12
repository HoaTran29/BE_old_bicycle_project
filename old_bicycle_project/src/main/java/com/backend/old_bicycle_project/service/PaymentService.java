package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.SepayWebhookRequestDTO;
import com.backend.old_bicycle_project.dto.response.PaymentRequestResponseDTO;
import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.User;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentRequestResponseDTO createUpfrontPaymentRequest(UUID orderId, User currentUser);

    List<PaymentResponseDTO> getOrderPayments(UUID orderId, User currentUser);

    void handleSepayWebhook(SepayWebhookRequestDTO requestDTO, String authorizationHeader);
}
