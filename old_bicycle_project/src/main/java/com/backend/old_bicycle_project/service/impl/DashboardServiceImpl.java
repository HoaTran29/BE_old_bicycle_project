package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.DashboardStatsDTO;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InspectionRepository inspectionRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        
        BigDecimal totalRevenueRaw = orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED);
        BigDecimal totalRevenue = totalRevenueRaw != null ? totalRevenueRaw : BigDecimal.ZERO;

        long totalInspections = inspectionRepository.count();
        long passedInspections = productRepository.countByStatus(ProductStatus.inspected_passed);
        long failedInspections = productRepository.countByStatus(ProductStatus.inspected_failed);

        List<Object[]> rawMonthlyRevenue = orderRepository.getMonthlyRevenue();
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        for (Object[] row : rawMonthlyRevenue) {
            String month = (String) row[0];
            BigDecimal amount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            monthlyRevenue.put(month, amount);
        }

        List<Object[]> rawMonthlyOrders = orderRepository.getMonthlyOrderCount();
        Map<String, Long> monthlyOrders = new LinkedHashMap<>();
        for (Object[] row : rawMonthlyOrders) {
            String month = (String) row[0];
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : Long.parseLong(row[1].toString());
            monthlyOrders.put(month, count);
        }

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalInspections(totalInspections)
                .passedInspections(passedInspections)
                .failedInspections(failedInspections)
                .monthlyRevenue(monthlyRevenue)
                .monthlyOrders(monthlyOrders)
                .build();
    }
}
