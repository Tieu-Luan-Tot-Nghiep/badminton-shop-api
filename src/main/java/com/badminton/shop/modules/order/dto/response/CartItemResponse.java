package com.badminton.shop.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
