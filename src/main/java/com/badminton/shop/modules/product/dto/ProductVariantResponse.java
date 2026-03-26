package com.badminton.shop.modules.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String weight;
    private String gripSize;
    private String stiffness;
    private String balancePoint;
    private String size;
    private String color;
    private Double price;
    private Integer stock;
    private Integer shippingWeightGrams;
    private Integer shippingLengthCm;
    private Integer shippingWidthCm;
    private Integer shippingHeightCm;
}
