package com.badminton.shop.modules.shipping.dto.request;

import com.badminton.shop.modules.auth.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOrderCreationRequest {
    private String clientOrderCode;
    private String receiverName;
    private String receiverPhone;
    private String note;
    private UserAddress address;
    private BigDecimal codAmount;
    private BigDecimal insuranceValue;
    private ShippingFeeCalculationRequest feeCalculationRequest;
}
