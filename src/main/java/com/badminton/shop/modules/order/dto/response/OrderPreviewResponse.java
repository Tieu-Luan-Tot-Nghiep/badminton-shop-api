package com.badminton.shop.modules.order.dto.response;

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
public class OrderPreviewResponse {
    @Builder.Default
    private List<OrderResponse.OrderLineResponse> items = new ArrayList<>();

    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal itemsAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
}
