package com.badminton.shop.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchTrendingItemResponse {
    private String keyword;
    private long count;
}
