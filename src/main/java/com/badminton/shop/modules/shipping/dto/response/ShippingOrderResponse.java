package com.badminton.shop.modules.shipping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOrderResponse {
    private String shippingCode;
    private BigDecimal shippingFee;
    private LocalDateTime expectedDeliveryTime;
    private String status;
    private String sortCode;
}
