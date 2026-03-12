package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByProductIdAndStatusIn(UUID productId, Collection<OrderStatus> statuses);

    List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(UUID buyerId, UUID sellerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    java.util.Optional<Order> findByIdAndBuyerId(UUID orderId, UUID buyerId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    // Grouping by function requires custom mapping in service or interface, 
    // let's fetch raw objects for simplicity if not using native query
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as month, SUM(total_amount) as revenue FROM orders WHERE status = 'completed' GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY month ASC", nativeQuery = true)
    List<Object[]> getMonthlyRevenue();

    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as month, COUNT(id) as order_count FROM orders WHERE status = 'completed' GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY month ASC", nativeQuery = true)
    List<Object[]> getMonthlyOrderCount();
}
