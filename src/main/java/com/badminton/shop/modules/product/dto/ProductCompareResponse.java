package com.badminton.shop.modules.product.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCompareResponse {
    private List<ProductCompareItemResponse> items;
}
