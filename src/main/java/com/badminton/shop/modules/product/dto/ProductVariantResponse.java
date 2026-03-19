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
    private String size;
    private String color;
    private Double price;
    private Integer stock;
}
