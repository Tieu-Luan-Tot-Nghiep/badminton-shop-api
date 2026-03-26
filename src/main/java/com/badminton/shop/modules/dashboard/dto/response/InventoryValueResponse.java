package com.badminton.shop.modules.dashboard.dto.response;

import java.math.BigDecimal;

public record InventoryValueResponse(
    Long totalStockQuantity,
    BigDecimal estimatedValue
) {}
