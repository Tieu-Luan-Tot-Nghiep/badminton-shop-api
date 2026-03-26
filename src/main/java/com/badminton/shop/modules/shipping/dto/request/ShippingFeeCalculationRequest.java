package com.badminton.shop.modules.shipping.dto.request;

import com.badminton.shop.modules.auth.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeCalculationRequest {
    private UserAddress address;
    private BigDecimal insuranceValue;

    @Builder.Default
    private List<ShippingItemRequest> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingItemRequest {
        private String name;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Integer weightGrams;
        private Integer lengthCm;
        private Integer widthCm;
        private Integer heightCm;
    }
}
