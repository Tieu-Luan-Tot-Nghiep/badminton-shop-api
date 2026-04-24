package com.badminton.shop.modules.promotion.dto.request;

import com.badminton.shop.modules.promotion.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {

    @NotBlank(message = "Promotion code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount value must be >= 0")
    private Double discountValue;

    @DecimalMin(value = "0.0", message = "Minimum order value must be >= 0")
    private Double minOrderValue;

    @DecimalMin(value = "0.0", message = "Max discount amount must be >= 0")
    private Double maxDiscountAmount;

    @Positive(message = "Max usage must be greater than 0")
    private Integer maxUsage;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean isActive = true;
}
