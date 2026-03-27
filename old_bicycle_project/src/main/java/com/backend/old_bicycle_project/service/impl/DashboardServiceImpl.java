package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.DashboardStatsDTO;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
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

        BigDecimal totalGmv = defaultZero(orderRepository.sumTotalAmountByStatus(OrderStatus.completed));
        BigDecimal pendingPlatformFee = defaultZero(
                orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.pending)
        );
        BigDecimal recognizedPlatformRevenue = defaultZero(
                orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.recognized)
        );
        BigDecimal reversedPlatformFee = defaultZero(
                orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.reversed)
        );

        long totalInspections = inspectionRepository.count();
        long passedInspections = productRepository.countByStatus(ProductStatus.inspected_passed);
        long failedInspections = productRepository.countByStatus(ProductStatus.inspected_failed);

        Map<String, BigDecimal> monthlyGmv = toBigDecimalMap(orderRepository.getMonthlyCompletedGmv());
        Map<String, BigDecimal> monthlyRecognizedPlatformRevenue =
                toBigDecimalMap(orderRepository.getMonthlyRecognizedPlatformRevenue());
        Map<String, Long> monthlyOrders = toLongMap(orderRepository.getMonthlyCompletedOrderCount());

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalGmv)
                .totalGmv(totalGmv)
                .pendingPlatformFee(pendingPlatformFee)
                .recognizedPlatformRevenue(recognizedPlatformRevenue)
                .reversedPlatformFee(reversedPlatformFee)
                .totalInspections(totalInspections)
                .passedInspections(passedInspections)
                .failedInspections(failedInspections)
                .monthlyRevenue(new LinkedHashMap<>(monthlyGmv))
                .monthlyGmv(monthlyGmv)
                .monthlyRecognizedPlatformRevenue(monthlyRecognizedPlatformRevenue)
                .monthlyOrders(monthlyOrders)
                .build();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> toBigDecimalMap(List<Object[]> rawRows) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rawRows) {
            String month = (String) row[0];
            BigDecimal amount = row[1] instanceof BigDecimal
                    ? (BigDecimal) row[1]
                    : new BigDecimal(row[1].toString());
            result.put(month, amount);
        }
        return result;
    }

    private Map<String, Long> toLongMap(List<Object[]> rawRows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rawRows) {
            String month = (String) row[0];
            Long count = row[1] instanceof Number
                    ? ((Number) row[1]).longValue()
                    : Long.parseLong(row[1].toString());
            result.put(month, count);
        }
        return result;
    }
}
