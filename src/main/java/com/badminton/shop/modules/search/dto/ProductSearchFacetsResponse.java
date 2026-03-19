package com.badminton.shop.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchFacetsResponse {
    private List<ProductSearchFacetBucketResponse> brands;
    private List<ProductSearchFacetBucketResponse> categories;
    private List<ProductSearchFacetBucketResponse> priceRanges;
}
