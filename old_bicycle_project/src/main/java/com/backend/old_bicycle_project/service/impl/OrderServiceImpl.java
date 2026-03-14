package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(User currentUser, OrderCreateRequestDTO requestDTO) {
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (product.getStatus() != ProductStatus.active && product.getStatus() != ProductStatus.inspected_passed) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (requestDTO.getDepositAmount().compareTo(product.getPrice()) > 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (orderRepository.existsByProductIdAndStatusIn(
                product.getId(),
                List.of(OrderStatus.pending, OrderStatus.deposited, OrderStatus.completed))) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        BigDecimal serviceFee = requestDTO.getServiceFee() != null ? requestDTO.getServiceFee() : BigDecimal.ZERO;

        Order order = orderRepository.save(Order.builder()
                .buyer(currentUser)
                .seller(product.getSeller())
                .product(product)
                .totalAmount(product.getPrice())
                .depositAmount(requestDTO.getDepositAmount())
                .serviceFee(serviceFee)
                .paymentMethod(requestDTO.getPaymentMethod())
                .status(OrderStatus.pending)
                .build());

        return mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(User currentUser) {
        List<Order> orders = currentUser.getRole() == AppRole.admin
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(currentUser.getId(), currentUser.getId());

        return orders.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmDeposit(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.deposited);
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO completeOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        if (order.getStatus() != OrderStatus.shipped) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.completed);
        order.getProduct().setStatus(ProductStatus.sold);
        productRepository.save(order.getProduct());
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO acceptOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        // Buyer must deposit first before seller can accept
        if (order.getStatus() != OrderStatus.deposited) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.accepted);
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO shipOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);
        validateSellerOrAdmin(order, currentUser);

        // Seller must accept before shipping
        if (order.getStatus() != OrderStatus.accepted) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.shipped);
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId, User currentUser) {
        Order order = getOrder(orderId);

        boolean canCancel = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId())
                || order.getSeller().getId().equals(currentUser.getId());
        if (!canCancel) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.completed || order.getStatus() == OrderStatus.cancelled) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        order.setStatus(OrderStatus.cancelled);
        return mapToDTO(orderRepository.save(order));
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
    }

    private void validateSellerOrAdmin(Order order, User currentUser) {
        boolean isSellerOrAdmin = currentUser.getRole() == AppRole.admin
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isSellerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private OrderResponseDTO mapToDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productTitle(order.getProduct().getTitle())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getFullName())
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .serviceFee(order.getServiceFee())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
