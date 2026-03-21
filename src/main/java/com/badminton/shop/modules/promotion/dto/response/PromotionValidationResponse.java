package com.badminton.shop.modules.promotion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationResponse {
    private String voucherCode;
    private Boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalShippingFee;
    private BigDecimal finalTotalAmount;
}
