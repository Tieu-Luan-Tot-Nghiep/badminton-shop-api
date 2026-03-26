package com.badminton.shop.modules.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCompareItemResponse {
    private Long productId;
    private String productName;
    private String slug;
    private String thumbnailUrl;
    private String brandName;

    private Long variantId;
    private String sku;
    private Double price;
    private String weight;
    private String gripSize;
    private String stiffness;
    private String balancePoint;
}
