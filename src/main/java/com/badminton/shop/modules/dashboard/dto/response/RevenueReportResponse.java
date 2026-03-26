package com.badminton.shop.modules.dashboard.dto.response;

import java.math.BigDecimal;

public record RevenueReportResponse(
    String period,
    BigDecimal totalRevenue,
    Long totalOrders
) {}
