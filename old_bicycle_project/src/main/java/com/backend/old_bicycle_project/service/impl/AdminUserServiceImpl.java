package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.AdminUserActivityResponseDTO;
import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.entity.Conversation;
import com.backend.old_bicycle_project.entity.Notification;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.Wishlist;
import com.backend.old_bicycle_project.entity.enums.AppRole;
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
import com.backend.old_bicycle_project.service.AdminUserService;
import com.backend.old_bicycle_project.specification.UserSpecification;
import com.backend.old_bicycle_project.validation.PaginationValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final WishlistRepository wishlistRepository;
    private final ConversationRepository conversationRepository;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final com.backend.old_bicycle_project.service.RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<AdminUserResponseDTO> getAllUsers(
            String keyword,
            AppRole role,
            UserStatus status,
            Boolean verified,
            int page,
            int size
    ) {
        var pageable = PaginationValidationUtils.createPageRequest(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(
                UserSpecification.fromAdminFilter(keyword, role, status, verified),
                pageable
        ).map(this::toResponse);
    }

    @Override
    public AdminUserResponseDTO getUserById(UUID userId) {
        return toResponse(getRequiredUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserStatus(UUID userId, UserStatus status, UUID adminId) {
        if (adminId != null && adminId.equals(userId)) {
            throw new AppException(ErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED);
        }

        User user = getRequiredUser(userId);
        user.setStatus(status);
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public String resetUserPassword(UUID userId, String newPassword) {
        passwordPolicyValidator.validate(newPassword);

        User user = getRequiredUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.deleteAllByUser(user);

        return "Admin da dat lai mat khau va thu hoi cac phien dang nhap cu.";
    }

    @Override
    public AdminUserActivityResponseDTO getUserActivity(UUID userId) {
        User user = getRequiredUser(userId);

        var topFive = PaginationValidationUtils.createPageRequest(0, 5, Sort.by("createdAt").descending());
        List<Product> recentProducts = productRepository.findBySellerIdAndDeletedAtIsNull(userId, topFive).getContent();
        List<Order> recentOrders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .limit(5)
                .toList();
        List<Report> recentReports = reportRepository.findByReporterIdOrderByCreatedAtDesc(userId, topFive).getContent();
        List<Notification> recentNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, topFive).getContent();

        return AdminUserActivityResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .verified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalProducts(productRepository.countBySellerId(userId))
                .totalOrdersAsBuyer(orderRepository.countByBuyerId(userId))
                .totalOrdersAsSeller(orderRepository.countBySellerId(userId))
                .totalReportsSubmitted(reportRepository.countByReporterId(userId))
                .totalWishlistItems(wishlistRepository.countByUserId(userId))
                .totalConversations(conversationRepository.countConversationsByUserId(userId))
                .unreadNotifications(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .recentProducts(recentProducts.stream().map(this::toRecentProductItem).toList())
                .recentOrders(recentOrders.stream().map(order -> toRecentOrderItem(order, userId)).toList())
                .recentReports(recentReports.stream().map(this::toRecentReportItem).toList())
                .recentNotifications(recentNotifications.stream().map(this::toRecentNotificationItem).toList())
                .recentWishlistItems(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .limit(5)
                        .map(this::toRecentWishlistItem)
                        .toList())
                .build();
    }

    private User getRequiredUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private AdminUserResponseDTO toResponse(User user) {
        return AdminUserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .defaultAddress(user.getDefaultAddress())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.isVerified())
                .averageRating(user.getAverageRating())
                .totalReviews(user.getTotalReviews())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AdminUserActivityResponseDTO.RecentProductItem toRecentProductItem(Product product) {
        return AdminUserActivityResponseDTO.RecentProductItem.builder()
                .productId(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private AdminUserActivityResponseDTO.RecentOrderItem toRecentOrderItem(Order order, UUID userId) {
        String involvement = order.getBuyer() != null && userId.equals(order.getBuyer().getId()) ? "buyer" : "seller";
        return AdminUserActivityResponseDTO.RecentOrderItem.builder()
                .orderId(order.getId())
                .productId(order.getProduct() != null ? order.getProduct().getId() : null)
                .productTitle(order.getProduct() != null ? order.getProduct().getTitle() : null)
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .involvement(involvement)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private AdminUserActivityResponseDTO.RecentReportItem toRecentReportItem(Report report) {
        return AdminUserActivityResponseDTO.RecentReportItem.builder()
                .reportId(report.getId())
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private AdminUserActivityResponseDTO.RecentNotificationItem toRecentNotificationItem(Notification notification) {
        return AdminUserActivityResponseDTO.RecentNotificationItem.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private AdminUserActivityResponseDTO.RecentWishlistItem toRecentWishlistItem(Wishlist wishlist) {
        return AdminUserActivityResponseDTO.RecentWishlistItem.builder()
                .productId(wishlist.getProduct().getId())
                .productTitle(wishlist.getProduct().getTitle())
                .productStatus(wishlist.getProduct().getStatus())
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
