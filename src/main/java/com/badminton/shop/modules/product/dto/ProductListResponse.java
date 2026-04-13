package com.badminton.shop.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO nhẹ cho danh sách sản phẩm (trang chủ, danh sách)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String thumbnailUrl;
    private BigDecimal basePrice;
    private Double rate;
    private String brandName;
    private String categoryName;
    private Boolean isActive;
    private Boolean isDeleted;
}
