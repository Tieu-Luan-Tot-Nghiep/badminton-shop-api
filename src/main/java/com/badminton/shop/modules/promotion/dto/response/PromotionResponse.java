package com.badminton.shop.modules.promotion.dto.response;

import com.badminton.shop.modules.promotion.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscountAmount;
    private Integer maxUsage;
    private Integer currentUsage;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private Boolean isActive;
}
