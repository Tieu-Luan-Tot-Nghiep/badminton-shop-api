package com.badminton.shop.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutAddressResponse {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private Boolean isDefault;
}
