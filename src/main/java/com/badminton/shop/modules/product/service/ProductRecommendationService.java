package com.badminton.shop.modules.product.service;

import com.badminton.shop.modules.product.dto.ProductRecommendationResponse;

public interface ProductRecommendationService {

    /**
     * Gợi ý sản phẩm tương tự khi xem chi tiết sản phẩm.
     * Dùng vector similarity (KNN) + AI insight.
     *
     * @param productId  ID sản phẩm đang xem
     * @param size       Số lượng gợi ý (default 6)
     * @param withAi     Có kèm AI insight không (default false để tránh latency)
     */
    ProductRecommendationResponse getRecommendations(Long productId, int size, boolean withAi);
}