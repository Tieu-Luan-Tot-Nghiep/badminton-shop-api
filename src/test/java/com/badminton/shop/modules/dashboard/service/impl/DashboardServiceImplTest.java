package com.badminton.shop.modules.dashboard.service.impl;

import com.badminton.shop.modules.dashboard.dto.response.BrandRevenueResponse;
import com.badminton.shop.modules.dashboard.dto.response.RevenueReportResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(entityManager);
    }

    @Test
    void getRevenueByPeriod_ShouldUseCompletedDeliveredFilter_AndMapResult() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("startDate"), any(LocalDateTime.class))).thenReturn(query);
        when(query.setParameter(eq("endDate"), any(LocalDateTime.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{"2026-04-10", new BigDecimal("1500000"), 2L}));

        List<RevenueReportResponse> result = dashboardService.getRevenueByPeriod(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                "DAY"
        );

        assertEquals(1, result.size());
        assertEquals("2026-04-10", result.get(0).period());
        assertEquals(new BigDecimal("1500000"), result.get(0).totalRevenue());
        assertEquals(2L, result.get(0).totalOrders());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("o.payment_status = 'COMPLETED' OR o.status = 'DELIVERED'"));
        assertTrue(sql.contains("'RETURN_REQUESTED', 'AWAITING_RETURN', 'RETURN_RECEIVED'"));
        assertTrue(sql.contains("o.payment_status <> 'REFUNDED'"));
    }

    @Test
    void getRevenueByBrand_ShouldUseCompletedDeliveredFilter_AndMapResult() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("startDate"), any(LocalDateTime.class))).thenReturn(query);
        when(query.setParameter(eq("endDate"), any(LocalDateTime.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{"Yonex", new BigDecimal("2200000"), 12L}));

        List<BrandRevenueResponse> result = dashboardService.getRevenueByBrand(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59)
        );

        assertEquals(1, result.size());
        assertEquals("Yonex", result.get(0).brandName());
        assertEquals(new BigDecimal("2200000"), result.get(0).totalRevenue());
        assertEquals(12L, result.get(0).itemsSold());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("o.payment_status = 'COMPLETED' OR o.status = 'DELIVERED'"));
        assertTrue(sql.contains("o.payment_status <> 'REFUNDED'"));
    }
}
