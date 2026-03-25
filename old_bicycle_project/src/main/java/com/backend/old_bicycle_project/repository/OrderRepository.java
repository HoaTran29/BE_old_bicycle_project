package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByProductIdAndStatusIn(UUID productId, Collection<OrderStatus> statuses);

    @Query("select distinct o.product.id from Order o where o.product.id in :productIds and o.status in :statuses")
    List<UUID> findLockedProductIdsByProductIdsAndStatuses(
            @Param("productIds") Collection<UUID> productIds,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(UUID buyerId, UUID sellerId);

    long countByBuyerId(UUID buyerId);

    long countBySellerId(UUID sellerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    java.util.Optional<Order> findByIdAndBuyerId(UUID orderId, UUID buyerId);

    List<Order> findByStatusAndFundingStatusAndPaymentDeadlineBefore(
            OrderStatus status,
            OrderFundingStatus fundingStatus,
            LocalDateTime paymentDeadline
    );

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.platformFeeTotal) FROM Order o WHERE o.platformFeeStatus = :status")
    BigDecimal sumPlatformFeeTotalByPlatformFeeStatus(@Param("status") PlatformFeeStatus status);

    @Query(value = """
            SELECT TO_CHAR(COALESCE(platform_fee_recognized_at, updated_at, created_at), 'YYYY-MM') AS month,
                   SUM(total_amount) AS gmv
            FROM orders
            WHERE status = 'completed'
            GROUP BY TO_CHAR(COALESCE(platform_fee_recognized_at, updated_at, created_at), 'YYYY-MM')
            ORDER BY month ASC
            """, nativeQuery = true)
    List<Object[]> getMonthlyCompletedGmv();

    @Query(value = """
            SELECT TO_CHAR(platform_fee_recognized_at, 'YYYY-MM') AS month,
                   SUM(platform_fee_total) AS recognized_platform_revenue
            FROM orders
            WHERE platform_fee_status = 'recognized'
              AND platform_fee_recognized_at IS NOT NULL
            GROUP BY TO_CHAR(platform_fee_recognized_at, 'YYYY-MM')
            ORDER BY month ASC
            """, nativeQuery = true)
    List<Object[]> getMonthlyRecognizedPlatformRevenue();

    @Query(value = """
            SELECT TO_CHAR(COALESCE(platform_fee_recognized_at, updated_at, created_at), 'YYYY-MM') AS month,
                   COUNT(id) AS order_count
            FROM orders
            WHERE status = 'completed'
            GROUP BY TO_CHAR(COALESCE(platform_fee_recognized_at, updated_at, created_at), 'YYYY-MM')
            ORDER BY month ASC
            """, nativeQuery = true)
    List<Object[]> getMonthlyCompletedOrderCount();
}
