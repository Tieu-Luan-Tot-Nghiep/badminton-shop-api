package com.badminton.shop.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchItemResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String thumbnailUrl;
    private Double basePrice;
    private String brandName;
    private String categoryName;
    private Boolean isActive;
}
