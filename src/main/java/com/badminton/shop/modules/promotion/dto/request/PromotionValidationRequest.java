package com.badminton.shop.modules.promotion.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationRequest {

    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    @NotNull(message = "Items amount is required")
    @DecimalMin(value = "0.0", message = "Items amount must be >= 0")
    private BigDecimal itemsAmount;

    @NotNull(message = "Shipping fee is required")
    @DecimalMin(value = "0.0", message = "Shipping fee must be >= 0")
    private BigDecimal shippingFee;
}
