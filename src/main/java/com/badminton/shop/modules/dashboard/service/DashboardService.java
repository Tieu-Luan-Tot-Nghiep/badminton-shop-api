package com.badminton.shop.modules.dashboard.service;

import com.badminton.shop.modules.dashboard.dto.response.*;
import java.time.LocalDateTime;
import java.util.List;

public interface DashboardService {
    List<RevenueReportResponse> getRevenueByPeriod(LocalDateTime startDate, LocalDateTime endDate, String groupBy);
    List<BrandRevenueResponse> getRevenueByBrand(LocalDateTime startDate, LocalDateTime endDate);
    List<TopSellingResponse> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit);
    InventoryValueResponse getInventoryValue();
}
