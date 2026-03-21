package com.badminton.shop.modules.promotion.service;

import com.badminton.shop.modules.promotion.dto.request.PromotionRequest;
import com.badminton.shop.modules.promotion.dto.response.PromotionApplyResult;
import com.badminton.shop.modules.promotion.dto.response.PromotionResponse;
import com.badminton.shop.modules.promotion.dto.response.PromotionValidationResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface PromotionService {

    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Long id, PromotionRequest request);

    PromotionResponse setPromotionActive(Long id, boolean active);

    PromotionResponse getPromotionByCode(String code);

    Page<PromotionResponse> getPromotions(int page, int size, Boolean activeOnly);

    PromotionValidationResponse validatePromotion(String voucherCode, BigDecimal itemsAmount, BigDecimal shippingFee);

    PromotionApplyResult applyPromotionForOrder(String voucherCode, BigDecimal itemsAmount, BigDecimal shippingFee);

    void publishPromotionUsage(Long promotionId, String orderCode);
}
