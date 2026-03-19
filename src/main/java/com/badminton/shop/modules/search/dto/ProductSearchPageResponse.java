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
public class ProductSearchPageResponse {
    private List<ProductSearchItemResponse> content;
    private ProductSearchFacetsResponse facets;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
