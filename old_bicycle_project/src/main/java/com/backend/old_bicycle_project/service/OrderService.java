package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.User;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponseDTO createOrder(User currentUser, OrderCreateRequestDTO requestDTO);

    List<OrderResponseDTO> getMyOrders(User currentUser);

    OrderResponseDTO acceptOrder(UUID orderId, User currentUser);

    OrderResponseDTO confirmDeposit(UUID orderId, User currentUser);

    OrderResponseDTO completeOrder(UUID orderId, User currentUser);

    OrderResponseDTO confirmReceived(UUID orderId, User currentUser);

    OrderResponseDTO cancelOrder(UUID orderId, User currentUser);
}
