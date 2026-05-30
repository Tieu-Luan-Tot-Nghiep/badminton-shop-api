package com.badminton.shop.modules.product.dto;

import com.badminton.shop.modules.search.dto.ProductSearchItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendationResponse {

    /** Sản phẩm đang xem */
    private Long sourceProductId;
    private String sourceProductName;

    /** Danh sách sản phẩm gợi ý (vector similarity) */
    private List<ProductSearchItemResponse> recommendations;

    /** AI insight giải thích tại sao gợi ý những sản phẩm này */
    private String aiInsight;

    /** Có dùng AI insight không */
    private boolean aiInsightEnabled;

    /** Tổng số gợi ý */
    private int total;
}