package com.badminton.shop.modules.promotion.dto.response;

import com.badminton.shop.modules.promotion.entity.Promotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionApplyResult {
    private Promotion promotion;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal finalShippingFee;
    private BigDecimal finalTotalAmount;
}
