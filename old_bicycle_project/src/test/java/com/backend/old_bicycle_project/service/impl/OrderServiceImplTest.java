package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderWithFullPaymentSetsUpfrontAmountToTotalPrice() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Specialized Allez")
                .price(new BigDecimal("12000000"))
                .status(ProductStatus.active)
                .build();

        OrderCreateRequestDTO request = OrderCreateRequestDTO.builder()
                .productId(product.getId())
                .paymentOption(PaymentOption.full)
                .paymentMethod(PaymentMethod.transfer)
                .build();

        when(productRepository.findById(product.getId())).thenReturn(java.util.Optional.of(product));
        when(orderRepository.existsByProductIdAndStatusIn(eq(product.getId()), any(List.class))).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        OrderResponseDTO response = orderService.createOrder(buyer, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getPaymentOption()).isEqualTo(PaymentOption.full);
        assertThat(savedOrder.getRequiredUpfrontAmount()).isEqualByComparingTo("12000000");
        assertThat(savedOrder.getDepositAmount()).isEqualByComparingTo("12000000");
        assertThat(savedOrder.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(savedOrder.getRemainingAmount()).isEqualByComparingTo("12000000");

        assertThat(response.getRequiredUpfrontAmount()).isEqualByComparingTo("12000000");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("12000000");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.transfer);
    }

    private User user(AppRole role, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .build();
    }
}
