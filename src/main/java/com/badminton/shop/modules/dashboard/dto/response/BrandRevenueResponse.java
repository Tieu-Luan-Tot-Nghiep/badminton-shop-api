package com.badminton.shop.modules.dashboard.dto.response;

import java.math.BigDecimal;

public record BrandRevenueResponse(
    String brandName,
    BigDecimal totalRevenue,
    Long itemsSold
) {}
