package com.badminton.shop.modules.dashboard.dto.response;

import java.math.BigDecimal;

public record TopSellingResponse(
    Long productId,
    String productName,
    String slug,
    String thumbnailUrl,
    Long totalQuantitySold,
    BigDecimal totalRevenue
) {}
