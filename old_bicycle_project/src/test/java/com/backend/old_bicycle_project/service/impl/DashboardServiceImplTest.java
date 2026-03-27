package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.DashboardStatsDTO;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardStatsSeparatesGmvFromPlatformRevenue() {
        when(userRepository.count()).thenReturn(12L);
        when(productRepository.count()).thenReturn(24L);
        when(orderRepository.count()).thenReturn(6L);
        when(orderRepository.sumTotalAmountByStatus(OrderStatus.completed))
                .thenReturn(new BigDecimal("20000000"));
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.pending))
                .thenReturn(new BigDecimal("100000"));
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.recognized))
                .thenReturn(new BigDecimal("350000"));
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.reversed))
                .thenReturn(new BigDecimal("50000"));
        when(inspectionRepository.count()).thenReturn(8L);
        when(productRepository.countByStatus(ProductStatus.inspected_passed)).thenReturn(5L);
        when(productRepository.countByStatus(ProductStatus.inspected_failed)).thenReturn(2L);
        when(orderRepository.getMonthlyCompletedGmv()).thenReturn(List.<Object[]>of(
                new Object[]{"2026-02", new BigDecimal("5000000")},
                new Object[]{"2026-03", new BigDecimal("15000000")}
        ));
        when(orderRepository.getMonthlyRecognizedPlatformRevenue()).thenReturn(List.<Object[]>of(
                new Object[]{"2026-03", new BigDecimal("350000")}
        ));
        when(orderRepository.getMonthlyCompletedOrderCount()).thenReturn(List.<Object[]>of(
                new Object[]{"2026-02", 1L},
                new Object[]{"2026-03", 5L}
        ));

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getTotalUsers()).isEqualTo(12L);
        assertThat(stats.getTotalProducts()).isEqualTo(24L);
        assertThat(stats.getTotalOrders()).isEqualTo(6L);
        assertThat(stats.getTotalGmv()).isEqualByComparingTo("20000000");
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("20000000");
        assertThat(stats.getPendingPlatformFee()).isEqualByComparingTo("100000");
        assertThat(stats.getRecognizedPlatformRevenue()).isEqualByComparingTo("350000");
        assertThat(stats.getReversedPlatformFee()).isEqualByComparingTo("50000");
        assertThat(stats.getMonthlyGmv())
                .containsEntry("2026-02", new BigDecimal("5000000"))
                .containsEntry("2026-03", new BigDecimal("15000000"));
        assertThat(stats.getMonthlyRevenue())
                .containsEntry("2026-02", new BigDecimal("5000000"))
                .containsEntry("2026-03", new BigDecimal("15000000"));
        assertThat(stats.getMonthlyRecognizedPlatformRevenue())
                .containsEntry("2026-03", new BigDecimal("350000"));
        assertThat(stats.getMonthlyOrders())
                .containsEntry("2026-02", 1L)
                .containsEntry("2026-03", 5L);
    }

    @Test
    void getDashboardStatsDefaultsNullMoneyMetricsToZero() {
        when(userRepository.count()).thenReturn(0L);
        when(productRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalAmountByStatus(OrderStatus.completed)).thenReturn(null);
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.pending)).thenReturn(null);
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.recognized)).thenReturn(null);
        when(orderRepository.sumPlatformFeeTotalByPlatformFeeStatus(PlatformFeeStatus.reversed)).thenReturn(null);
        when(inspectionRepository.count()).thenReturn(0L);
        when(productRepository.countByStatus(ProductStatus.inspected_passed)).thenReturn(0L);
        when(productRepository.countByStatus(ProductStatus.inspected_failed)).thenReturn(0L);
        when(orderRepository.getMonthlyCompletedGmv()).thenReturn(Collections.emptyList());
        when(orderRepository.getMonthlyRecognizedPlatformRevenue()).thenReturn(Collections.emptyList());
        when(orderRepository.getMonthlyCompletedOrderCount()).thenReturn(Collections.emptyList());

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getTotalGmv()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getPendingPlatformFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getRecognizedPlatformRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getReversedPlatformFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getMonthlyGmv()).isEmpty();
        assertThat(stats.getMonthlyRevenue()).isEmpty();
        assertThat(stats.getMonthlyRecognizedPlatformRevenue()).isEmpty();
        assertThat(stats.getMonthlyOrders()).isEmpty();
    }
}
