package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.AdminUserActivityResponseDTO;
import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.entity.Notification;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.Wishlist;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ConversationRepository;
import com.backend.old_bicycle_project.repository.NotificationRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReportRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.repository.WishlistRepository;
import com.backend.old_bicycle_project.service.PasswordPolicyValidator;
import com.backend.old_bicycle_project.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void getAllUsersReturnsPagedMappedResults() {
        User user = user(UUID.randomUUID(), "buyer@test.dev", AppRole.buyer, UserStatus.active);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserResponseDTO> response = adminUserService.getAllUsers("buyer", AppRole.buyer, UserStatus.active, true, 0, 12);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getEmail()).isEqualTo("buyer@test.dev");
        assertThat(response.getContent().getFirst().getRole()).isEqualTo(AppRole.buyer);
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(userId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void updateUserStatusRejectsChangingOwnStatus() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> adminUserService.updateUserStatus(adminId, UserStatus.banned, adminId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED));

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatusPersistsNewStatus() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = user(userId, "seller@test.dev", AppRole.seller, UserStatus.active);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponseDTO response = adminUserService.updateUserStatus(userId, UserStatus.banned, adminId);

        assertThat(response.getStatus()).isEqualTo(UserStatus.banned);
        assertThat(user.getStatus()).isEqualTo(UserStatus.banned);
        verify(userRepository).save(eq(user));
    }

    @Test
    void resetUserPasswordUpdatesHashAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "buyer@test.dev", AppRole.buyer, UserStatus.active);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("StrongPass2")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        String message = adminUserService.resetUserPassword(userId, "StrongPass2");

        assertThat(message).contains("Admin da dat lai mat khau");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        verify(passwordPolicyValidator).validate("StrongPass2");
        verify(refreshTokenService).deleteAllByUser(user);
    }

    @Test
    void getUserActivityBuildsSummaryAndRecentItems() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "seller@test.dev", AppRole.seller, UserStatus.active);
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .title("Bianchi Sprint")
                .price(BigDecimal.valueOf(25000000))
                .status(com.backend.old_bicycle_project.entity.enums.ProductStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .buyer(user(UUID.randomUUID(), "buyer@test.dev", AppRole.buyer, UserStatus.active))
                .seller(user)
                .product(product)
                .status(OrderStatus.pending)
                .totalAmount(BigDecimal.valueOf(25000000))
                .createdAt(LocalDateTime.now())
                .build();
        Report report = Report.builder()
                .id(UUID.randomUUID())
                .reporter(user)
                .targetId(UUID.randomUUID())
                .targetType("PRODUCT")
                .status(ReportStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .title("Có đơn hàng mới")
                .type(NotificationType.system)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.countBySellerId(userId)).thenReturn(3L);
        when(orderRepository.countByBuyerId(userId)).thenReturn(1L);
        when(orderRepository.countBySellerId(userId)).thenReturn(4L);
        when(reportRepository.countByReporterId(userId)).thenReturn(2L);
        when(wishlistRepository.countByUserId(userId)).thenReturn(5L);
        when(conversationRepository.countConversationsByUserId(userId)).thenReturn(6L);
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(2L);
        when(productRepository.findBySellerIdAndDeletedAtIsNull(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId))
                .thenReturn(List.of(order));
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(wishlist));

        AdminUserActivityResponseDTO response = adminUserService.getUserActivity(userId);

        assertThat(response.getTotalProducts()).isEqualTo(3);
        assertThat(response.getUnreadNotifications()).isEqualTo(2);
        assertThat(response.getRecentProducts()).hasSize(1);
        assertThat(response.getRecentOrders()).hasSize(1);
        assertThat(response.getRecentWishlistItems()).hasSize(1);
    }

    private User user(UUID id, String email, AppRole role, UserStatus status) {
        return User.builder()
                .id(id)
                .email(email)
                .role(role)
                .status(status)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
